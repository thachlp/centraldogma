.. _mirroring:

Better configuration change workflow with Git-to-CD mirroring
=============================================================
Making a configuration change is often a risky business. Pushing an invalid configuration change can cause your
service to malfunction even if zero line of code is changed. To reduce the chance of outage due to incorrect
configuration changes, you would want them reviewed by many eyes before they are applied.

Modern source code hosting services such as `GitHub <https://github.com/>`_ and `GitLab <https://about.gitlab.com/>`_
have a notion of `pull request <https://help.github.com/articles/about-pull-requests/>`_. What if we use pull
requests for configuration changes just like we do for source code, given the importance of service
configuration?

With Central Dogma's periodic Git repository mirroring, you can set up the following workflow in your
organization:

- Humans work on a Git repository to manage the configuration files.

  1. Store the configuration files in a Git repository.
  2. Send a pull request that updates the configuration files.
  3. The pull request is reviewed and merged.

- Applications work on a Central Dogma repository to retrieve the configuration files.

  1. Central Dogma mirrors the configuration files in the Git repository into a Central Dogma repository.
  2. Applications watches the configuration files in the Central Dogma repository.

Note that the applications do not access Git repositories directly. There are a few good reasons to make your
applications access Central Dogma repositories instead:

- Source code repositories are often hosted in a different network.
- Source code repositories are not always highly-available, although they may be backed up regularly.
- Central Dogma repositories are highly-available, queryable and watchable.

Setting up a Git-to-CD mirror
-----------------------------
You need to put two files into the ``meta`` repository of your Central Dogma project:
``/mirrors/{mirror-id}.json`` and ``/credentials/{credential-id}.json``.

Setting up a mirroring task
^^^^^^^^^^^^^^^^^^^^^^^^^^^

``/mirrors/{mirror-id}.json`` contains an object of a periodic mirroring task. For example:

.. code-block:: json
   :caption: foo-settings-mirror.json

   {
     "id": "foo-settings-mirror",
     "enabled": true,
     "schedule": "0 * * * * ?",
     "direction": "REMOTE_TO_LOCAL",
     "localRepo": "foo",
     "localPath": "/",
     "remoteUri": "git+ssh://git.example.com/foo.git/settings#release",
     "credentialId": "my_private_key",
     "gitignore": [
         "/credential.txt",
         "private_dir"
     ],
     "zone": "zone1"
   }

- ``id`` (string)

  - the ID of the mirroring task. You should set the same value specified in the file name.
    For example, if the file name is ``/mirrors/foo-settings-mirror.json``, the value of this field should be
    ``foo-settings-mirror``.

- ``enabled`` (boolean, optional)

  - whether the mirroring task is enabled. Enabled by default if unspecified.

- ``schedule`` (string, optional)

  - a `Quartz cron expression <https://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/crontrigger.html>`_
    that describes when the mirroring task is supposed to be triggered. If unspecified, ``0 * * * * ?``
    (every minute) is used.

- ``direction`` (string)

  - the direction of mirror. Use ``REMOTE_TO_LOCAL``.

- ``localRepo`` (string)

  - the Central Dogma repository name. The content under the location specified in ``remoteUri`` will be
    mirrored into this repository.

- ``localPath`` (string, optional)

  - the directory path in ``localRepo``. The content under the location specified in ``remoteUri`` will be
    mirrored into this directory in ``localRepo``. If unspecified, ``/`` is used.

- ``remoteUri`` (string)

  - the URI of the Git repository which will be mirrored from.
  - Supported schemes are:

    - ``git+http``
    - ``git+https``
    - ``git+ssh``

  - Path is split after ``.git``. The part after ``.git`` refers the directory inside the Git repository.
    e.g. ``/foo.git/src/settings`` refers to the files under the directory ``/src/settings`` which resides in
    the Git repository ``/foo.git`` If you want to mirror the whole content of the repository, you can simply
    end the URI with ``.git``. e.g. ``git+ssh://git.example.com/foo.git``
  - Fragment represents a branch name. e.g. ``#release`` will mirror the branch ``release``. If unspecified,
    the repository's default branch is mirrored.

- ``credentialId`` (string)

  - the ID of the credential to use for authentication, as defined in ``/credentials/{credential-id}.json``.

- ``gitignore`` (string or array of strings, optional)

  - a `gitignore <https://git-scm.com/docs/gitignore>` specifies files that should be excluded from mirroring.
    The type of gitignore can either be a string containing the entire file (e.g. ``/filename.txt\ndirectory``) or an array 
    of strings where each line represents a single pattern. The file pattern expressed in gitignore is relative to the
    path of ``remoteUri``.

- ``zone`` (string, optional)

   - the zone where the mirroring task is executed.

   - If unspecified:

     - a mirroring task is executed in the first zone of ``zone.allZones`` configuration.
     - if ``zone.allZones`` is not configured, a mirroring task is executed in the leader replica.

Setting up a credential
^^^^^^^^^^^^^^^^^^^^^^^

``/credentials/{credential-id}.json`` contains the authentication credential which is required when accessing
the Git repositories defined in ``/mirrors/{mirror-id}.json``:

* No authentication
.. code-block:: json
   :caption: no_auth.json

   {
     "id": "no_auth",
     "type": "none"
   }

* Password-based authentication
.. code-block:: json
   :caption: my_password.json

   {
     "id": "my_password",
     "type": "password",
     "username": "alice",
     "password": "secret!"
   }

* SSH public key authentication
.. code-block:: json
   :caption: my_private_key.json

   {
     "id": "my_private_key",
     "type": "public_key",
     "username": "git",
     "publicKey": "ssh-ed25519 ... user@host",
     "privateKey": "-----BEGIN OPENSSH PRIVATE KEY-----\n...\n-----END OPENSSH PRIVATE KEY-----\n",
     "passphrase": null
   }

* Access token-based authentication
.. code-block:: json
   :caption: my_access_token.json

   {
     "id": "my_access_token",
     "type": "access_token",
     "accessToken": "github_pat_..."
   }

- ``id`` (string)

  - the ID of the credential. You should set the same value specified in the file name. For example, if the file
    name is ``/credentials/my_private_key.json``, the value of this field should be ``my_private_key``.
    You can specify the value of this field in the ``credentialId`` field of the mirror definitions in
    ``/mirrors/{mirror-id}.json``.

- ``type`` (string)

  - the type of authentication mechanism: ``none``, ``password``, ``public_key`` or ``access_token``.

- ``username`` (string)

  - the user name. You must specify this field if you use a credential whose type is ``password`` or
    ``public_key``.

- ``password`` (string)

  - the password which is used for password-based authentication.

- ``publicKey`` (string)

  - the OpenSSH RSA, ECDSA or EdDSA public key which is used for SSH public key authentication.

- ``privateKey`` (string)

  - the OpenSSH RSA, ECDSA or EdDSA private key which is used for SSH public key authentication.
    The PEM format is also supported.

    .. tip::

        You can convert your private key into a JSON string using a ``perl`` command:

        .. code-block:: shell

            $ cat ~/.ssh/id_rsa | perl -p -0 -e 's/\r?\n/\\n/g'

- ``passphrase`` (string)

  - the passphrase of ``privateKey`` if the private key is encrypted.
    If unspecified or ``null``, the private key should not be encrypted.

- ``accessToken`` (string)

  - the access token which is used for access token-based authentication such as
    `GitHub Personal Access Token <https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/creating-a-personal-access-token>`_.

If everything was configured correctly, the repository you specified in ``localRepo`` will have a file named
``mirror_state.json`` on a successful run, which contains the commit ID of the Git repository:

.. code-block:: json

    {
      "sourceRevision": "22fb176e4d8096d709d34ffe985c5f3acea83ef2"
    }

Setting up a CD-to-Git mirror
-----------------------------
It's exactly the same as setting up a Git-to-CD mirror which is described above, except you need to specify
``direction`` with ``LOCAL_TO_REMOTE``.

Mirror limit settings
---------------------
Central Dogma limits the number of files and the total size of the files in a mirror for its reliability.
As your configuration grows, you may want to bump the limit. See :ref:`setup-configuration` to learn about
the options related with mirroring: ``numMirroringThreads``, ``maxNumFilesPerMirror`` and
``maxNumBytesPerMirror``.
