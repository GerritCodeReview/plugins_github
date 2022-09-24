
GitHub configuration during Gerrit init
---------------------------------------

This plugin provides a customized Gerrit init step for the self-configuration of
the main GitHub and Gerrit authentication settings for allowing the github-oauth
library to work properly.

GitHub OAuth library rely on Gerrit HTTP authentication defined during the standard
Gerrit init steps.
See below a sample session of relevant init steps for a default
configuration pointing to the Web GitHub instance:

```
   *** GitHub Integration
   *** 

   GitHub URL                     [https://github.com]: 
   GitHub API URL                 [https://api.github.com]: 

   NOTE: You might need to configure a proxy using http.proxy if you run Gerrit behind a firewall.

   *** GitHub OAuth registration and credentials
   *** 

   Register Gerrit as GitHub application on:
   https://github.com/settings/applications/new

   Settings (assumed Gerrit URL: http://localhost:8080/)
   * Application name: Gerrit Code Review
   * Homepage URL: http://localhost:8080/
   * Authorization callback URL: http://localhost:8080/oauth

   After registration is complete, enter the generated OAuth credentials:
   GitHub Client ID               [1ebea047915210179cf5]: 
   ClientSecret                   []: f82c3f9b3802666f2adcc4c8cacfb164295b0a99
   confirm password : 
   HTTP Authentication Header     [GITHUB_USER]: 
```

Configuration
-------------

GitHub plugin read his configuration from gerrit.config under the `[github]` section.

github.url
:   GitHub homepage URL. Default is `https://github.com`. Can be customized to a different 
    value for GitHub:Enterprise integration

**NOTE** You might need to configure a proxy using the configuration `http.proxy` if you run
Gerrit behind a firewall.

github.apiUrl
:   GitHub API URL. Default is `https://api.github.com`. Can be customized to a different 
    value for GitHub:Enterprise integration

github.clientId
:   The `Client ID`, that was received from GitHub when the application was registered. Required.

github.clientSecret
:   The `Client Secret`, that was received from GitHub when the application was registered. Required.

github.scopes
:   The GitHub scopes for allowing access to the user's public or private profile, organisations and 
    repositories. See [GitHub Scopes definition](https://developer.github.com/v3/oauth/#scopes) 
    for a detailed description of available scopes and their associated permissions. 
    Default is empty read-only access to public 
    information (includes public user profile info, public repository info, and gists).

github.httpConnectionTimeout
:   Maximum time to wait for GitHub API to answer to a new HTTP connection attempt.
    Values should use common common unit unit suffixes to express their setting:
    * ms, milliseconds
    * s, sec, second, seconds
    * m, min, minute, minutes
    * h, hr, hour, hours
    Default value: 30 seconds

github.httpReadTimeout
:   Maximum time to wait for GitHub API to respond or send data over an existing HTTP connection.
    Values should use common common unit unit suffixes to express their setting:
    * ms, milliseconds
    * s, sec, second, seconds
    * m, min, minute, minutes
    * h, hr, hour, hours
    Default value: 30 seconds

Key Configuration
-------------

Since this plugin obtains credentials from Github and persists them in Gerrit,
it also takes care of encrypting them at rest. The Gerrit admin can configure
how this is done by setting the relevant configuration parameters.

github-key.<key-id>.passwordDevice
: The device or file where to retrieve the encryption passphrase.\
This is a required parameter for `key-id` configuration.

github-key.<key-id>.passwordLength
: The length in bytes of the password read from the passwordDevice.\
Default: 16

github-key.<key-id>.cipherAlgorithm
: The algorithm to be used for encryption/decryption. Available algorithms are
described in
the [Cipher section](https://docs.oracle.com/en/java/javase/11/docs/specs/security/standard-names.html#cipher-algorithm-names)
of the Java Cryptography Architecture Standard Algorithm Name Documentation.\
Default: AES/ECB/PKCS5Padding

github-key.<key-id>.secretKeyAlgorithm
: the algorithm to be used to encrypt the provided password. Available
algorithms are described in
the [Cipher section](https://docs.oracle.com/en/java/javase/11/docs/specs/security/standard-names.html#cipher-algorithm-names)
of the Java Cryptography Architecture Standard Algorithm Name Documentation.\
Default: AES

github-key.<key-id>.current
: Whether this configuration is the current one, and it should be used to
encrypt new Github credentials. Note that _exactly_ one github-key configuration
must be set to `current`, otherwise an error exception will be thrown.\
Default: false

As you can observe, in order to support key rotations, multiple `github-key`
can be specified in configuration. credentials encrypted with a `<key-id>` key
can still be decrypted as long as the `github-key.<key-id>` stanza is available
in the configuration. New credentials will always be encrypted with
the `current` `<key-id>`.

Note that unencrypted oauth tokens will be handled gracefully and just passed
through to github by the decryption algorithm. This is done so that oauth tokens
that were persisted _before_ the encryption feature was implemented will still
be considered valid until their natural expiration time.

If no `github-key.<key-id>` exists in configuration, then a default current key
configuration (named `current`) will be created. In this case, new 16 bytes
long password will be generated and stored to `$site/data/@PLUGIN@/default.key`
file which will be returned as `passwordDevice`. The remaining parameters will
be inferred from the above defaults.
