Plugin @PLUGIN@
===============

This plugins allows to integrate Gerrit with external set of users configured
on GitHub.
It relies on the installation of the github-oauth Java library under the $GERRIT_SITE/lib
in order filter all the HTTP requests through the GitHub OAuth 2.0 secure authentication.

GitHub init step
----------------

This plugin provides a customized Gerrit init step for the self-configuration of
the main GitHub and Gerrit authentication settings for allowing the github-oauth
library to work properly.

GitHub OAuth library rely on Gerrit HTTP authentication defined during the standard
Gerrit init steps.
See below a sample session of relevant init steps for a default
configuration pointing to the Web GitHub instance:

	*** User Authentication
	***

	Authentication method          []: HTTP
	Get username from custom HTTP header [Y/n]? Y
	Username HTTP header           []: GITHUB_USER
	SSO logout URL                 : /oauth/reset


	*** GitHub Integration
	***

	GitHub URL                     [https://github.com]:
	Use GitHub for Gerrit login ?  [Y/n]? Y
	ClientId                       []: 384cbe2e8d98192f9799
	ClientSecret                   []: f82c3f9b3802666f2adcc4c8cacfb164295b0a99
