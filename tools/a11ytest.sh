on() { settings put secure enabled_accessibility_services de.lumen.home/.RemoteService; settings put secure accessibility_enabled 1; sleep 3; }
chk() { echo "$1: [$(settings get secure enabled_accessibility_services)]"; }
on; chk start
input keyevent 3; sleep 3; chk home1
input keyevent 22; sleep 2; chk right
input keyevent 23; sleep 4; chk open-fav
input keyevent 3; sleep 3; chk home-from-app
input keyevent 3; input keyevent 3; sleep 4; chk double-on-home
input keyevent 4; sleep 2; chk back
sleep 15; chk later15
