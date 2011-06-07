import xmlrpclib

# Import the config options set by the user
from config import serverAddress, serverPort, controlPassword

s = xmlrpclib.ServerProxy('http://%s:%s' % (serverAddress, serverPort))

global serverState

def parse(cmd):
	global serverState
	if (cmd == 't'):
		s.setState(controlPassword, not s.getState())
		if s.getState():
			print '--- Server is now collecting data.'
		else:
			print '--- Server is no longer collecting data.'
	elif (cmd == 's'):
		if s.getState():
			print '--- Server is collecting data.'
		else:
			print '--- Server is not collecting data.'
	elif(cmd == 'q'):
			exit()

def run():
	global serverState
	
	try:
		serverState = s.getState()
	except Exception:
		print 'Check your server address and port in config.py, and try again.'
		exit()
	
	print '---'
	parse('s')
	print '---'
	print ''
			
	while True:
		print '[ s ] Get server status.'
		print '[ t ] Toggle server state.'
		print '[ q ] Quit (does not effect server).'

		cmd = raw_input('> ')
		parse(cmd)
		
		print ''
		
run()
