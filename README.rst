An ENSIME_ SideKick plugin for jEdit.  Note that this plugin operates very much
like the equivalent Emacs mode: it starts the ENSIME server as a sub-process and
initializes individual projects based on explicit user interaction.  This was
done very intentionally, since the Emacs mode of interaction is more flexible and
certainly more amenable to one-off file editing.

.. _ENSIME: http://aemon.com/file_dump/ensime_manual.html


Features
========

* Error reporting (type checking on save)
* Inspect type (determinine what type is inferred for a particular symbol)

Er...that's about it right now.  :-)  I have a proof-of-concept for popup
completion (intellisense), but it's going to take a fair amount of effort to make
it *actually* functional.  I know what to do, but it's not trivial.  And it's not
particularly important to me, so I probably won't do it unless I'm bored.  If anyone
is interested, I'll lay out the roadmap for what needs to be done and let them
handle the implementation.

Oh, if you want to contribute, feel free!  I'm going to be fairly liberal with
merging pull requests.  I can commit to maintaining the parts of this project
that I rely on for my day-to-day work.  Everything else is caveat emptor.


Installing
==========

Right now, installing the plugin requires building it from scratch.  Fortunately,
the building part is really easy...provided your machine is set up *identically*
to mine.  The build is based on SBT, but it currently hard-codes the locations
of the following three JAR files:

* ``jedit.jar``
* ``SideKick.jar``
* ``ErrorList.jar``

Factoring this out into something configurable is one of the big things that needs
to be done to make this plugin *not* specific to my laptop.  Once you have this
overcome, you should be able to run ``sbt package`` to build the plugin JAR.
After that, you can use the ``local-deploy.sh`` script to install the plugin.
Unfortunately, this too is specific to my machine.  Less so, but still using
hard-coded paths.  It should be very obvious how to make this process more generic
though, or even to perform the installation yourself.  All you need to do is
copy the packaged JAR as well as ``scala-library.jar`` into the ``jEdit/jars/``
directory.  Once that is done, restart jEdit.

Now here's the tricky part.  The ``scala.sys.process`` package doesn't provide any
mechanism for sending signals to processes.  As a result, I had to do something
*very* hacky in order to shut down the ENSIME server on plugin unload.  This
hack may currently be viewd on line 69 of ``BackendComponent.scala``.  Basically,
it invokes a subprocess which spawns a shell and runs a little scriptlet that
determines the ENSIME server PID using ``pgrep`` and sends the ``HUP`` signal to
that process.  This is probably the most serious machine-specific part of the
code, since (for PATH reasons) I had to hard-code the location of the ``pgrep``
executable on my system.  If someone could come up with a more elegant way of
killing the SBT process, I would be eternally grateful!


Usage
=====

You *must* have a pre-existing ``.ensime`` file at the root of your project.  If
you don't, the server init will probably do something very strange.  At the very
least, it won't work.  The ENSIME server process will be spawned the moment you
open a Scala file (assuming you have the "ensime" parser associated with the Scala
mode in the SideKick plugin options).  For debugging purposes, it is useful to
run ``tail -f /tmp/ensime*log`` and watch the server's output in real time.  This
isn't necessary though.

To initialize your project, run the ``ensime`` command within jEdit.  This is most
easily done via the action bar (bound to C+ENTER by default) but can also be done
via the Plugins > ENSIME > Initialize Project menu item.  This action will recursively
scan up the directory hierarchy from your currently focused open file and attempt
to find a ``.ensime`` file.  If it is successful, the dialog will be populated
with the parent directory for that file.  If not, then you will need to enter
the path manually.  It will *not* generate this file for you!  (that may come later)

ENSIME messages are displayed in the status bar.  Once the status bar displays
"ENSIME: Indexer ready", you should be ready to go with development.  Assuming
your ``.ensime`` file is properly configured, you should now have type-check-on-save
behavior in Scala files.  Also, you should be able to inspect the type for the
symbol under the cursor.  This is done via the Plugins > ENSIME > Inspect Type
action (I have this bound to A+s A+t).  The type is displayed in the status bar.
All types should be displayed in a *reasonably* friendly format, though I haven't
yet implemented special formatting support for arrow types (functions).  These
types will display, but they'll be a little weird to read.

Popup completion is currently disabled, for which you should be very grateful.
The implementation is in ``EnsimeParser.scala`` and ``EnsimeProtocolComponent.scala``
if you're interested in improving it.  Please read ENSIME's ``SwankProtocol.scala``
before you jump in, since it will help you understand why this is such a thorny
feature, particularly in jEdit.


ENSIME Client
=============

One of the consequences of this effort is a generic ENSIME client implemented in
Scala.  It's not a full client yet, but I'm adding onto it bit by bit and trying
to keep it as feature-agnostic as possible.  Hopefully the source should prove at
least a useful starting point for someone trying to work with the ENSIME server
in Scala.
