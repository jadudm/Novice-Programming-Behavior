from SimpleXMLRPCServer import SimpleXMLRPCServer
from SimpleXMLRPCServer import SimpleXMLRPCRequestHandler
# Any Python 2.4/2.5/2.6 installation should have these.
import os.path, sqlite3, md5, re, base64, time, math

# Import the config options set by the user
from config import serverAddress, serverPort, dataPath
from config import debugServer, controlPassword

##################################################################################
# NO EDITS PAST THIS LINE ########################################################
##################################################################################

# This DB REVISION number is only to maintain parity with the 
# old Scheme/Snooze-based implementation. Arbitrarily, I've 
# set the revision number to be 20081225.
global db_revision
db_revision = 20081225

if (os.path.isdir(dataPath)):
	print '--- Storing data to "%s"' % dataPath
else:
	print '--- Data path "%s" does not exist. Exiting!' % dataPath
	exit()
print '--- Starting server for IP address %s on port %s' % (serverAddress, serverPort)

#########################################
# XML-RPC SERVER SETUP
#########################################
# Restrict to a particular path.
class RequestHandler(SimpleXMLRPCRequestHandler): 
    rpc_paths = ('/RPC2',)

# Create server
server = SimpleXMLRPCServer((serverAddress, serverPort), 
                            requestHandler=RequestHandler)
server.register_introspection_functions()

#########################################
# OPERATION FLAG
#########################################
# This is just a simple boolean flag that
# gets turned on and off by the remote control.
# The server always starts in a "running" state.
serverRunning = True


#########################################
# 'debug' AND 'error' SUPPORT
#########################################
def debug(str):
	global debugServer
	if debugServer:
		print "[DEBUG] %s" % str

def error(str):
	print "======= [ERROR] ======="
	print str
	print "======================="

#########################################
# SERVER HELPERS AND "insert" METHOD
#########################################

# 
# prep_data
# Adds a timestamp to the user-supplied data before
# stuffing it into a database.
def prep_data(fields, types, data):
	
	# Add the TIMESTAMP field to the list of user-supplied fields
	fields.insert(0, 'TIMESTAMP')	
	# Add the INT type to the list of types
	types.insert(0, 'integer')
		
	# Insert the current time into the data dictionary
	data['TIMESTAMP'] = math.floor(time.time())
	return data

#
# missing
# Like a membership test, but flipped.
def missing (ls, val):
	result = False
	if (ls.count(val) == 0): 
		result = True
	return result
	
#
# sanity_check
# Like the name says. Makes sure that 
# the XML-RPC call has the right information
# to successfully insert into a DB.
def sanity_check (fields, types, data):
	result = True
	# First, make sure I have the same number of 
	# fields and types.
	if (len(fields) != len(types)):
		if(len(fields) > len(types)):
			error('SANITY CHECK: More fields than types')
		else:
			error('SANITY CHECK: More types than fields')
		result = False
	
	# Now, make sure I have data in the hash
	# for every field.
	for field in fields:
		if (missing(data.keys(), field)):
			print "Missing " + field + " in dictionary ", data.keys()
			result = False
			break
	
	return result

#
# sqliteFile
# Returns the full path to the SQLite
# database.
def sqliteFile(name):
	global dataPath
	return dataPath + name

#
# tableSpec
# Builds a table specification that can be used
# with the CREATE TABLE SQL command.
def tableSpec (fields, types):
	ls = []	
	for f, t in zip(fields, types):
		if (t == 'boolean'):
			ls.append(f + ' integer')
		else:
			ls.append(f + ' ' + t)
	
	comma = []
	for i in range(0, len(fields)):
		comma.append(ls[i])
		if (not (i == len(fields) - 1)):
			comma.append(',')
	
	spec = ''
	for e in comma:
		spec = spec + e
	
	return spec
	
# 
# createTable
# Creates an SQLite table in the database
# based on the fields and types given.
def createTable(conn, cur, name, fields, types):
	s = 'create table ' + name
	s = s + ' ('
	s = s + 'id integer primary key autoincrement, revision integer, '
	s = s + tableSpec(fields, types) 
	s = s + ')'
	
	debug('Creating table')
	debug('%s' % s)
	cur.execute(s)
	conn.commit()

#
# hashDBName
# Creates a unique DB name, and along the way
# also strips spaces from the name the user provided.
# In short, a bit of sanity work is done.
def hashDBName (name, fields):
	# Append all the fields
	s = ''
	for f in fields:
		s = s + f

	# MD5 the field names
	m = md5.new()
	m.update(s)
	
	# Remove spaces from the user-supplied name
	name = re.sub(' ', '', name)
	
	# Return the munged DB name
	return name + '-' + m.hexdigest()
	
#
# doInsert
# Inserts an event into the database.
# Assumes the DB exists... which should be the 
# case, since I created the database if it didn't 
# in the "writeData" function call.
def doInsert (conn, cur, name, fields, types, data):
	debug('Doing insert of %s fields into %s.' % (len(fields), name))
	
	s = ''
	# Here, I'm inserting NULL and the DB REVISION to keep the 
	# table consistent with the old Snooze-based table implementation.
	s = 'insert into ' + name + ' values (NULL, %s, ' % db_revision

	i = 0
	for f, t in zip(fields, types):
		try:
			if   (t == 'integer'):
				s = s + '%s' % data[f]
			elif (t == 'text'):
				str = '%s' % data[f]
				# Strings need to be Base64 encoded or SQLite 
				# chokes on Unicode characters.
				s = s + '\"%s\"' % base64.b64encode(str)
			elif (t == 'boolean'):
				if (data[f] == False):
					s = s + '0'
				else:
					s = s + '1'
			else:
				error('Bad type found in doInsert.')
				
		except Exception:
			error('Spec build error (%s).' % e)
			
		if (not (i == len(fields) - 1)):
			s = s + ','
			i = i + 1
			
	s = s + ');'
	
	debug('Doing Insert')
	debug('%s' % s)
	try:
		cur.execute(s)
	except Exception:
		error('Error in insert.')
		
	try:
		conn.commit()
	except Exception:
		error('Error in commit.')
	
#
# writeData
# Stores data into an SQLite database
def writeData (name, fields, types, data):
	# Hash the DB name, so if the fields change,
	# I create a new DB file in the directory.
	hashName = hashDBName(name, fields) + '.sqlite'
	
	debug('Hashed DB name is "%s".' % hashName)
	
	# If the file doesn't exist, I'll have to create it.
	if (not os.path.isfile(sqliteFile(hashName))):
		# Open a new connection
		debug('Opening connection to create table.')
		conn = sqlite3.connect(sqliteFile(hashName))
		cur = conn.cursor()
		# Create the table; 
		createTable(conn, cur, name, fields, types)
		doInsert(conn, cur, name, fields, types, data)
		debug('Closing connection after creating table.')
		cur.close()
	# Otherwise, just initialize a cursor for use.
	else:
		debug('Opening connection to insert data.')
		conn = sqlite3.connect(sqliteFile(hashName))
		cur = conn.cursor()
		doInsert(conn, cur, name, fields, types, data)
		debug('Closing connection after inserting data.')
		cur.close()

#########################################
# IT ALL STARTS HERE
#########################################
def store (name, fields, types, data):
	result = True
	
	# Only if we're running
	if serverRunning:
		data = prep_data(fields, types, data)

		# Do a sanity check; we'll bail if this fails.
		checkResult = sanity_check(fields, types, data)		
	
		if (checkResult):
			writeData(name, fields, types, data)
		else:
			result = checkResult
		
	return result

#########################################
# REMOTE CONTROL
#########################################
def setRunState(password, state):
	global serverRunning
	global controlPassword
	
	if(password == controlPassword):
		print 'Setting run state to %s' % state
		serverRunning = state
		
	return serverRunning
	
def getRunState():
	global serverRunning
	return serverRunning
	
#########################################
# RPC REGISTRATION AND STARTUP
#########################################
server.register_function(store, 'insert')
server.register_function(setRunState, 'setState')
server.register_function(getRunState, 'getState')
server.serve_forever()
