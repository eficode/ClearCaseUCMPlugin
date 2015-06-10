cleartool mkvob -tag \Cool_PVOB -ucmproject -c "The PVOB for the Cool test environment" -stgloc -auto
cleartool mkvob -tag \Cool -c "The Client VOB, which hosts the ordinay components in the Cool test environment" -stgloc -auto
cleartool mkcomp -c "The Cool top level component" -nroot _System@\Cool_PVOB
cleartool mkcomp -c "The Client sub system component" -nroot _Client@\Cool_PVOB
cleartool mkcomp -c "The Server sub system component" -nroot _Server@\Cool_PVOB
cleartool mount \Cool
cleartool mkview -tag Cool_baseview -stgloc -auto
cd /d m:\Cool_baseview\Cool
cleartool mkcomp -c "The Trace Component" -root Trace Trace@\Cool_PVOB
cleartool mkcomp -c "The ServerTest Component" -root ServerTest ServerTest@\Cool_PVOB
cleartool mkcomp -c "The GUI Component" -root Gui Gui@\Cool_PVOB
cleartool mkcomp -c "The Model Component" -root Model Model@\Cool_PVOB
cleartool mkproject -c "Boot strap project for Cool" -modcomp Trace@\Cool_PVOB,ServerTest@\Cool_PVOB,Gui@\Cool_PVOB,Model@\Cool_PVOB -model SIMPLE -policy POLICY_INTERPROJECT_DELIVER -in RootFolder CoolBootStrap@\Cool_PVOB
cleartool mkstream -integration -in CoolBootStrap@\Cool_PVOB -nc -baseline _System_INITIAL@\Cool_PVOB,_Client_INITIAL@\Cool_PVOB,_Server_INITIAL@\Cool_PVOB,Model_INITIAL@\Cool_PVOB,Trace_INITIAL@\Cool_PVOB,Gui_INITIAL@\Cool_PVOB,ServerTest_INITIAL@\Cool_PVOB CoolBootStrap_int@\Cool_PVOB
cleartool mkview -tag CoolBootStrap_int -stream CoolBootStrap_int@\Cool_PVOB -stgloc -auto
cd /d m:\CoolBootStrap_int
cleartool mkbl -c "intermediate bl to add structure to _Server" -component _Server@\Cool_PVOB -adepends_on ServerTest@\Cool_PVOB,Model@\Cool_PVOB,Trace@\Cool_PVOB Server_intermediate
cleartool mkbl -c "intermediate bl to add structure to _Client" -component _Client@\Cool_PVOB -adepends_on Model@\Cool_PVOB,Gui@\Cool_PVOB Client_intermediate
cleartool mkbl -c "First Structure" -component _System@\Cool_PVOB -adepends_on _Server@\Cool_PVOB,_Client@\Cool_PVOB -full Structure_initial
cleartool mkbl -c "First Structure" -component _System@\Cool_PVOB -identical -full Structure_1_0
cleartool mkproject -c "Mainline project for System" -modcomp Trace@\Cool_PVOB,ServerTest@\Cool_PVOB,Gui@\Cool_PVOB,Model@\Cool_PVOB -policy POLICY_INTERPROJECT_DELIVER,POLICY_DELIVER_REQUIRE_REBASE,POLICY_DELIVER_NCO_DEVSTR,POLICY_CHSTREAM_UNRESTRICTED -in RootFolder System_Mainline@\Cool_PVOB
cleartool mkstream -integration -in System_Mainline@\Cool_PVOB -nc -baseline Structure_1_0@\Cool_PVOB System_Mainline_int@\Cool_PVOB
cleartool mkproject -c "Docking (or trunk) project for the Server team" -modcomp Trace@\Cool_PVOB,ServerTest@\Cool_PVOB,Model@\Cool_PVOB -policy POLICY_CHSTREAM_UNRESTRICTED,POLICY_INTERPROJECT_DELIVER,POLICY_DELIVER_REQUIRE_REBASE,POLICY_DELIVER_NCO_DEVSTR -in RootFolder Server@\Cool_PVOB
cleartool mkstream -integration -in Server@\Cool_PVOB -nc -baseline Structure_1_0@\Cool_PVOB -target System_Mainline_int@\Cool_PVOB Server_int@\Cool_PVOB
cleartool mkproject -c "Docking (or trunk) project for the Client team" -modcomp Gui@\Cool_PVOB -policy POLICY_CHSTREAM_UNRESTRICTED,POLICY_INTERPROJECT_DELIVER,POLICY_DELIVER_REQUIRE_REBASE,POLICY_DELIVER_NCO_DEVSTR -in RootFolder Client@\Cool_PVOB
cleartool mkstream -integration -in Client@\Cool_PVOB -nc -baseline Structure_1_0@\Cool_PVOB -target System_Mainline_int@\Cool_PVOB Client_int@\Cool_PVOB



