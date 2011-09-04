An ENSIME_ SideKick plugin for jEdit.  Note that this plugin operates very much
like the equivalent Emacs mode: it starts the ENSIME server as a sub-process and
initializes individual projects based on explicit user interaction.  This was
done very intentionally, since the Emacs mode of interaction is more flexible and
certainly more amenable to one-off file editing.

.. _ENSIME: http://aemon.com/file_dump/ensime_manual.html


Features
========

* Error reporting (type checking on save)
* Inspect type (determine what type is inferred for a particular symbol)
* Jump to declaration

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
directory.  Once that is done, restart jEdit (or rescan the plugins directory
using the Plugin Manager and load the ENSIME plugin manually).

Note that ENSIME has historically played a little sloppy with its sub-processes.
There is currently a pull request open to fix this, but it won't rectify matters
for older versions of ENSIME.  Fortunately, it's a pretty easy issue to fix in
your local ENSIME setup.  Simply open the ``bin/server`` file in your ENSIME
distribution and prefix the last line of the script with ``exec``.  This will
cause ENSIME to load ``java`` in a single process, rather than spawning a parent
shell process which loads ``java`` as a child.  If you fail to perform this step,
jEdit will be unable to automatically kill the ENSIME process *at all* and you
will need to manually cleanup orphaned server processes.  The easiest way to do
this is to run ``ps aux | grep ensime`` and use ``kill`` on any results.


Usage
=====

You *must* have a pre-existing ``.ensime`` file at the root of your project.  If
you don't, the server init will probably do something very strange.  At the very
least, it won't work.  For debugging purposes, it is useful to run 
``tail -f /tmp/ensime*log`` and watch the server's output in real time.  This
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

It's worth noting that ENSIME seems much happier with most functionality *after*
it has done a typecheck on the project.  jEdit won't trigger this automatically
on startup, so if you're trying out the functionality immediately after initializing
the ENSIME project, you may want to save a file (to trigger the typecheck) before
running any of the actual ENSIME actions.  This is only required once (in fact,
ENSIME actions *should* work just fine even in a dirty buffer once the type
checking has been run).

Oh, and Sidekick has some strange glitches with respect to typechecking files.
The most reliable way I've found to keep it happy is to do two things.  First,
dock the "Sidekick" view in your sidebar and ensure that you have opened it at
least once in your current editing session.  You don't need to leave it open,
just docked.  Then, after you start ENSIME, switch buffers once and then come
back to the buffer you actually wanted to edit.  This will force Sidekick to
trigger a type checking and everything should line up from that point.  Type
checking happens by default on buffer switch and on save (this is something you
configure in Sidekick).

Popup completion is currently disabled, for which you should be very grateful.
The implementation is in ``EnsimeParser.scala`` and ``EnsimeProtocolComponent.scala``
if you're interested in improving it.  Please read ENSIME's ``SwankProtocol.scala``
before you jump in, since it will help you understand why this is such a thorny
feature, particularly in jEdit.

Just in case the ENSIME server starts doing something weird, you can always
restart it.  This is done by first invoking the ``ensime.kill`` command and then
re-running ``ensime``.


ENSIME Client
=============

One of the consequences of this effort is a generic ENSIME client implemented in
Scala.  It's not a full client yet, but I'm adding onto it bit by bit and trying
to keep it as feature-agnostic as possible.  Hopefully the source should prove at
least a useful starting point for someone trying to work with the ENSIME server
in Scala.
