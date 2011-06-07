import xmlrpclib, time
from datetime import datetime

# Import the config options set by the user
from config import serverAddress, serverPort, controlPassword, sleepTime

s = xmlrpclib.ServerProxy('http://%s:%s' % (serverAddress, serverPort))

def safeGet():
	msg = 'ERROR'

	try:
		
		if s.getState():
			msg = 'Collecting'
		else:
			msg = 'Idle'

	except Exception:
		msg = 'SERVER DOWN'

	return msg

def run():
	while True:
		d = datetime.today()
		tim = d.replace(microsecond=0).isoformat(' ')
		msg = safeGet()			
		print '[%s] %s' % (tim, msg)
		time.sleep(sleepTime)
		
run()
