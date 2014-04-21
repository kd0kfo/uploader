#!/usr/bin/env bash

# Install prepreqs
yum -y install postgresql-server httpd perl php php-pdo sqlite sqlite-devel python python-setuptools vim-enhanced emacs elinks gcc gcc-c++ java java-devel make git subversion

easy_install install pip

# Setup services
chkconfig postgresql on
service postgresql initdb
service postgresql start

chkconfig httpd on
service httpd start

# Setup Files
cd /vagrant
if [[ ! -d repo ]];then
	git clone https://github.com/kd0kfo/webfs.git repo
fi
cd repo/web

for i in *.example;do
	cp $i ${i/\.example/}
done
\cp -R . /var/www/html/
cd /var/www/html
mkdir content
chown apache content
mkdir content/uploads
chown apache content/uploads

# Setup database
cat site_db.inc.example |sed 's/test.sqlite/\/tmp\/webfs.db/' > site_db.inc
php site_db.inc
sqlite3 /tmp/webfs.db "insert into users values ('foo', 'oFK9VQlmV8FA4U1QP4lq9M/JPcRDERWWpZNE/b12PPg=', NULL, NULL, 0);"

# Create html files
./build_pages.sh build

# Dump system info

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
