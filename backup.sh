BACKUPFILE=../mesibo-kotlin-$(date +%d%b%y-%H%M).bz2
tar --exclude='./app/build' --exclude='./backup'  -jcvf $BACKUPFILE .  

