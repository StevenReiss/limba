#! /bin/csh -f

set WD = `pwd`

ant relayjar

pm2 stop limbarelay

cat < /dev/null > $WD/relay.log

pm2 start --log $WD/relay.log --name limbarelay $WD/runrelay.sh
pm2 save
