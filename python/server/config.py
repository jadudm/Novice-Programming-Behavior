#########################################
# ONLY MODIFY THESE VARIABLES
#########################################
# You should only need to modify these 
# variables for the collection of data.

# Choose the IP address of the server that
# is hosting this script. "localhost" will 
# prevent machines elsewhere on the 
# network from logging data.
serverAddress = 'localhost'

# Choose the port that you wish to run the 
# server on. Anything over 1024 should be fine.
serverPort = 8080

# Set the full path to the directory where you
# will be storing data. If you run this server
# as user 'eva', then you will need to make sure
# that the user 'eva' has permissions to write 
# to the directory you indicate here.
dataPath   = '/tmp/ncb/'

# If you want to turn on debugging information,
# set this flag to True
debugServer = False

# Remote operation password
# If you want to be able to start and stop the server 
# remotely, then you'll need to set this
# "password"
controlPassword = 'changeme'
