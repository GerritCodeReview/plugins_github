GitHub plugin
=============

Integration between Gerrit and an external repository on GitHub.

Integration points
------------------

Provides the basic integration points for working effectively
with Gerrit as code-review tool for an existing repository hosted
on GitHub:

1. Authentication. (WIP)

Users can login to Gerrit using the same username and credentials
in GitHub. Gerrit login points to GitHub for generating the SSO token
to be used for the code-review authenticated session.

2. Push-Pull replication. (TODO)

Existing GitHub repositories are automatically replicated to Gerrit
for the purpose of performing code-review and pushing back changes
once approved. Additionally to the standard Gerrit push replication,
supports as well the ability to pull branches from remote GitHub
repositories.

3. Pull-request to Change. (TODO)

Hooks into the GitHub pull-request mechanism to automatically create
a Change in Gerrit submitted for review.
