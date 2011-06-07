import xmlrpclib, random, time
from config import serverAddress, serverPort, controlPassword, sleepTime

s = xmlrpclib.ServerProxy('http://%s:%s' % (serverAddress, serverPort))

possibleFields = ['admin', 'sysadm', 'sysadmin', 'operator', 'manager', 
				'lotus', 'lotus123', 'adm', 'anon', 'uucp', 'nuucp', 'device', 'devadmin', 
				'save', 'tar', 'support', 'custsup', 'database', 'catalog', 'user', 'tour', 
				'anonymous', 'guest', 'sysdiag', 'sysdiags', 'diags', 'test', 'diag', 'fld', 
				'service', 'visitor', 'demo', 'friend', 'library', 'syslib', 'print', 'spooler', 
				'lpadmin', 'lp', 'sysmaint', 'mgr', 'man', 'sysmgr', 'sysman', 'ncr', 'network', 
				'inst', 'install', 'net', 'netmgr']

alphabet = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
					  'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z']

fields = []
types  = []
data   = dict()

numberOfFields = 0


def randomString():
	s = ''
	for i in range(0, random.randint(20, 60)):
		s = s + alphabet[random.randint(0, (len(alphabet) - 1))]
	return s

def reset():
	global fields
	global types
	global data
	global numberOfFields
	fields = []
	types  = []
	data   = dict()
	random.seed(time.time())
	numberOfFields = random.randint(0, len(possibleFields) - 1)
	print "Generating [%s] fields." % numberOfFields
	
def initTypes():
	global numberOfFields
	global fields
	global types
	# print "Initializing %s types." % numberOfFields
	for i in range(0, numberOfFields):
		# Get a random field name
		fields.append(possibleFields[i])
		# And a random type. Favor text fields.
		if (random.randint(0,100) > 80):
			types.append('integer')
		else:
			types.append('text')
		
def initFields():
	global types
	global fields
	global data
	# print "Types[%s] Fields[%s]" % (len(types), len(fields))
	i = 0 
	for e in fields:
		if (types[i] == 'integer'):
			data[e] = random.randint(100, 10000)
		else:
			data[e] = randomString()
		i = i + 1

def run():
	global data
	global types
	global fields
	for n in range(0, 30):
		reset()
		initTypes()
		initFields()
		for i in range(0, 50):
			time.sleep(sleepTime)
			dbNumber = random.randint(0, 4)
			db = 'TestDB_%s' % dbNumber
			print "Test [%s - %s]" % (n, i)
			s.insert(db, fields, types, data) 

def testStateToggle():
	s.setState(controlPassword, False)
	global data
	global types
	global fields
	for n in range(0, 30):
		reset()
		initTypes()
		initFields()
		for i in range(0, 30):
			dbNumber = random.randint(0, 4)
			if(dbNumber == 4):
				s.setState(controlPassword, True)
				db = 'TestDB_%s' % dbNumber
				print "Test [%s - %s]" % (n, i)
				# print "Inserting %s" % data
				s.insert(db, fields, types, data) 
				time.sleep(sleepTime)
				s.setState(controlPassword, False)
	
run()
testStateToggle()

# Print list of available methods
#print s.system.listMethods()
