#!/bin/bash
# !/bin/bash -x to turn echo on 

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"
cd ${DIR}/..

# Start the webserver - http://127.0.0.1:8000/
java -jar bin/nextvideo.jar&
WS_PID=$!

cd bin
# Create a stop.sh script to kill the running instances
echo "#!/bin/bash" > stop.sh
chmod +x stop.sh
echo "# this script is automatically created - from start.sh"
echo "kill -9 ${WS_PID}" >> stop.sh
echo "***********************"
echo "run ${DIR}/stop.sh to stop the servers"