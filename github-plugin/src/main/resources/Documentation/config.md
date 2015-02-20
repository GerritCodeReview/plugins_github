
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
