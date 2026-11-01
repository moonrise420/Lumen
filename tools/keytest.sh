chk() { echo "$1: $(settings get secure enabled_accessibility_services) / bound=$(dumpsys accessibility | grep -c 'Bound services:{Service')"; }
settings put secure enabled_accessibility_services de.lumen.home/.RemoteService
settings put secure accessibility_enabled 1
sleep 3; chk start
input keyevent 22; sleep 2; chk right
input keyevent 21; sleep 2; chk left
input keyevent 3; sleep 3; chk home
input keyevent 3; input keyevent 3; sleep 3; chk double
input keyevent --longpress 3; sleep 3; chk long
dumpsys window | grep -m1 mCurrentFocus
