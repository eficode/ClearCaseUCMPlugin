cleartool mkvob -tag \2Cool_PVOB -ucmproject -c "The PVOB for the 2Cool test environment" -stgloc -auto
cleartool mkvob -tag \2Cool -c "The Client VOB, which hosts the ordinay components in the 2Cool test environment" -stgloc -auto
cleartool mkcomp -c "The 2Cool top level component" -nroot _System@\2Cool_PVOB
cleartool mkcomp -c "The Client sub system component" -nroot _Client@\2Cool_PVOB
cleartool mkcomp -c "The Server sub system component" -nroot _Server@\2Cool_PVOB
cleartool mount \2Cool
cleartool mkview -tag 2Cool_baseview -stgloc -auto
cd /d m:\2Cool_baseview\2Cool
cleartool mkcomp -c "The Trace Component" -root Trace Trace@\2Cool_PVOB
cleartool mkcomp -c "The ServerTest Component" -root ServerTest ServerTest@\2Cool_PVOB
cleartool mkcomp -c "The GUI Component" -root Gui Gui@\2Cool_PVOB
cleartool mkcomp -c "The Model Component" -root Model Model@\2Cool_PVOB
cleartool mkproject -c "Boot strap project for 2Cool" -modcomp Trace@\2Cool_PVOB,ServerTest@\2Cool_PVOB,Gui@\2Cool_PVOB,Model@\2Cool_PVOB -model SIMPLE -policy POLICY_INTERPROJECT_DELIVER -in RootFolder 2CoolBootStrap@\2Cool_PVOB
cleartool mkstream -integration -in 2CoolBootStrap@\2Cool_PVOB -nc -baseline _System_INITIAL@\2Cool_PVOB,_Client_INITIAL@\2Cool_PVOB,_Server_INITIAL@\2Cool_PVOB,Model_INITIAL@\2Cool_PVOB,Trace_INITIAL@\2Cool_PVOB,Gui_INITIAL@\2Cool_PVOB,ServerTest_INITIAL@\2Cool_PVOB 2CoolBootStrap_int@\2Cool_PVOB
cleartool mkview -tag 2CoolBootStrap_int -stream 2CoolBootStrap_int@\2Cool_PVOB -stgloc -auto
cd /d m:\2CoolBootStrap_int
cleartool mkbl -c "intermediate bl to add structure to _Server" -component _Server@\2Cool_PVOB -adepends_on ServerTest@\2Cool_PVOB,Model@\2Cool_PVOB,Trace@\2Cool_PVOB Server_intermediate
cleartool mkbl -c "intermediate bl to add structure to _Client" -component _Client@\2Cool_PVOB -adepends_on Model@\2Cool_PVOB,Gui@\2Cool_PVOB Client_intermediate
cleartool mkbl -c "First Structure" -component _System@\2Cool_PVOB -adepends_on _Server@\2Cool_PVOB,_Client@\2Cool_PVOB -full Structure_initial
cleartool mkbl -c "First Structure" -component _System@\2Cool_PVOB -identical -full Structure_1_0
cleartool mkproject -c "Mainline project for System" -modcomp Trace@\2Cool_PVOB,ServerTest@\2Cool_PVOB,Gui@\2Cool_PVOB,Model@\2Cool_PVOB -policy POLICY_INTERPROJECT_DELIVER,POLICY_DELIVER_REQUIRE_REBASE,POLICY_DELIVER_NCO_DEVSTR,POLICY_CHSTREAM_UNRESTRICTED -in RootFolder System_Mainline@\2Cool_PVOB
cleartool mkstream -integration -in System_Mainline@\2Cool_PVOB -nc -baseline Structure_1_0@\2Cool_PVOB System_Mainline_int@\2Cool_PVOB
cleartool mkproject -c "Docking (or trunk) project for the Server team" -modcomp Trace@\2Cool_PVOB,ServerTest@\2Cool_PVOB,Model@\2Cool_PVOB -policy POLICY_CHSTREAM_UNRESTRICTED,POLICY_INTERPROJECT_DELIVER,POLICY_DELIVER_REQUIRE_REBASE,POLICY_DELIVER_NCO_DEVSTR -in RootFolder Server@\2Cool_PVOB
cleartool mkstream -integration -in Server@\2Cool_PVOB -nc -baseline Structure_1_0@\2Cool_PVOB -target System_Mainline_int@\2Cool_PVOB Server_int@\2Cool_PVOB
cleartool mkproject -c "Docking (or trunk) project for the Client team" -modcomp Gui@\2Cool_PVOB -policy POLICY_CHSTREAM_UNRESTRICTED,POLICY_INTERPROJECT_DELIVER,POLICY_DELIVER_REQUIRE_REBASE,POLICY_DELIVER_NCO_DEVSTR -in RootFolder Client@\2Cool_PVOB
cleartool mkstream -integration -in Client@\2Cool_PVOB -nc -baseline Structure_1_0@\2Cool_PVOB -target System_Mainline_int@\2Cool_PVOB Client_int@\2Cool_PVOB



