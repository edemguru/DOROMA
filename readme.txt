General:
========
The code is ready and all requests are (hopefully correct) implemented.
There are still a lot of debug-messages which could help on testing (and rating) the system but should be removed for the final version.
Tests are done manually.


Nodes:
------
There are 4 modules:

1) Node:
This is the main node module which offers a user interface and controls all node threads.

2) UDP Node Sender:
This thread uses a loop with a timed sleep to send alive-messages via UDP in a fix interval.

3) TCP Node Listener:
This thread listen for incoming TCP connections from the cloud controller and starts new TCP Node Controller threads for each of them.

4) TCP Node Controller:
This thread answers a single compute request sent by the cloud controller, logs the result and terminates afterwards.

All 3 threads register themselves in a list on the main Node module. 
Every task has access to all items of this list. 
The Node uses this list to shut down all running threads on exit.


Cloud Controller:
-----------------
There are 6 modules:

1) Cloud Controller:
This is the main controller module which offers a user interface and also controls all controller modules

2) UDP Controller Listener:
This thread listens for incoming alive-messages from nodes and generates new Controller Node objects for each of them.

3) Controller Node.
For each node a Controller Node object keeps all current node data, like active-state, supported operations and usage.
If an alive-message arrives:
*) the node is marked as active, 
*) if running, an active timer-thread is stopped and 
*) a new timer-thread is started, which will set the node inactive after the timeout passed, before it ends itself.
For each new compute-request this module starts a new TCP Controller Client thread.

4) TCP Controller Node:
For each compute-request a new TCP Controller Node is started which connects to the node, forwards the compute-request, waits for the answer and closes itself afterwards.

5) TCP Controller Listener:
This thread listen for incoming TCP connections from the client and starts new TCP Controller Client threads for each of them.

6) TCP Controller Client:
For each client a new TCP Controller Client is started which handles the communication with the client.
After login a new Controller User object is generated to hold user specific data.
Compute-requests are forwarded via the Controller Node objects to new TCP Controller Node threads.

All threads and objects register themselves in a list on the main Cloud Controller module. 
Every task has access to all items of this list. 
The Cloud Controller uses this list to shut down all running threads on exit.


Client:
-------
There is only one module which offers a user interface and also communicates with the TCP Controller Client module.
