#!/usr/bin/env bash

yum -y install postgresql-server httpd perl php php-pdo sqlite sqlite-devel python vim-enhanced emacs elinks gcc gcc-c++ java java-devel make git subversion

chkconfig postgresql on
service postgresql initdb
service postgresql start

chkconfig httpd on
service httpd start

cd /vagrant
if [[ ! -d repo ]];then
	git clone https://github.com/kd0kfo/webfs.git repo
fi
cd repo/web

for i in *.example;do
	cp $i ${i/\.example/}
done
\cp $(git ls-files .) /var/www/html/
\cp site* /var/www/html/
cd /var/www/html
mkdir content
chown apache content
mkdir content/uploads
chown apache content/uploads
php site_db.inc
sqlite3 /tmp/uploader.php "insert into users values ('foo', 'oFK9VQlmV8FA4U1QP4lq9M/JPcRDERWWpZNE/b12PPg=', NULL, NULL, 0);"

CREATETAG=$(echo This machine was setup using vagrant on $(date +"%Y-%m-%d %H:%M"))
CREATEFILE=/etc/hostinfo.created
echo $CREATETAG > $CREATEFILE
echo >> $CREATEFILE
echo >> $CREATEFILE
echo It started with these packages >> $CREATEFILE
rpm -qa >> $CREATEFILE

echo $CREATETAG > /etc/motd
echo >> /etc/motd
echo Very convenient. >> /etc/motd
echo Then again it is Linux. >> /etc/motd
echo So no surprise. w00t! >> /etc/motd
