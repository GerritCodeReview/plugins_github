
This plugins allows to integrate Gerrit with external set of users configured
on GitHub.
It relies on the installation of the github-oauth Java library under the `$GERRIT_SITE/lib`
in order filter all the HTTP requests through the GitHub OAuth 2.0 secure authentication.

GitHub application registration
-------------------------------

GitHub uses OAuth2 as protocol to allow external apps request authorization to private
details in a user's GitHub account without getting their password. This is
preferred over Basic Authentication because tokens can be limited to specific
types of data, and can be revoked by users at any time.

Site owners have to register their application before getting started.  For
more information see [GitHub application registration page](https://github.com/settings/applications/new).
A registered OAuth application is assigned a unique `Client ID` and `Client
Secret`. The `Client Secret` should never be shared.

The Gerrit OAuth callback `<gerrit canonical URL>/oauth`
needs to be specified in the GitHub application registration to establish mutual
trust between the two domains and exchange the authorization codes. The use of HTTPS
for Gerrit is strongly recommended for keeping the secrets exchange confidential.

`auth.httpHeader` is set to `GITHUB_USER` with this authentication method and `auth.type`
must be set to HTTP.