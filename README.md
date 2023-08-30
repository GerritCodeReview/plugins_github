GitHub plugin
=============
This plugin allows existing GitHub repositories to be integrated
as Gerrit projects.

Why using GitHub and Gerrit together ?
--------------------------------------

Many people see Gerrit and GitHub as opposites: the pull-request
model adopted by GitHub is often used as "easy shortcut" to the
more comprehensive and structured code-review process in Gerrit.

There are many discussion threads on this:
- http://programmers.stackexchange.com/questions/173262/gerrit-code-review-or-githubs-fork-and-pull-model
- http://stackoverflow.com/questions/2451644/gerrit-with-github
- http://julien.danjou.info/blog/2013/rant-about-github-pull-request-workflow-implementation

In reality there are already OpenSource projects that have started
using the two tools together:
- OpenStack (https://wiki.openstack.org/wiki/GerritJenkinsGithub)
- MediaWiki (http://www.mediawiki.org/wiki/Gerrit)

The reason for using GitHub and Gerrit together are:
a) GitHub is widely recognised and accessible by lots of world-wide sites.
b) Using a public GitHub repo allows to "off-load" a lot of git pull traffic.
c) Pull-request allows novice users to start getting involved.
d) Gerrit code-review define the quality gates for avoiding "noise" of unstructured
   contributions.

Why a Gerrit plugin ?
---------------------

When using GitHub and Gerrit together, the "master of truth" has to be
Gerrit: this is because it is the place where more control in terms of
security and workflow can be defined.

A Gerrit plugin can help controlling the GitHub replica and importing
the pull requests as Gerrit changes.


Integration points
------------------

### Authentication. (DONE - Change-Id: I7291a90014e51316af3cb07fd03785c1ef33acd0)

Users can login to Gerrit using the same username and credentials
in GitHub. Gerrit login points to GitHub for generating the OAuth token
to be used for the code-review authenticated session.

The initial Gerrit registration page can be customised to import
GitHub SSH Keys directly into Gerrit.

### Push-Pull replication. (DONE - Change-Id: I596b2e80b4d9519668a1ab289d6c950139d6a922)

Existing GitHub repositories are automatically replicated to Gerrit
for the purpose of performing code-review and pushing back changes
once approved. Additionally to the standard Gerrit push replication,
supports as well the ability to pull branches from remote GitHub
repositories.

### Pull-request to Change. (DONE - Change-Id: d669e351a03798cc2ec966d028458f083c232564)

Hooks into the GitHub pull-request mechanism to automatically create
a Change in Gerrit submitted for review.

How to build this plugin
------------------------

### Gerrit 3.3 build

GitHub plugin is designed to work with Gerrit 3.3 (currently in development).
In order to build the GitHub plugin you need to have a working Gerrit 3.3
build in place.

See https://gerrit-review.googlesource.com/Documentation/dev-bazel.html for a
reference on how to build Gerrit using Bazel.

Gerrit 3.3 is distributed for Java 11 only. However, the source code is compatible
with Java 8 assuming you build it from the source repository by yourself.

The GitHub plugin can be built for Java 8 by using the `javaVersion=1.8` Maven
parameter.

Example:
  git clone https://gerrit.googlesource.com/plugins/github
  cd github
  mvn -DjavaVersion=1.8 install

### singleusergroup plugin

You need to install the singleusergroup plugin for Gerrit
(see https://gerrit-review.googlesource.com/#/admin/projects/plugins/singleusergroup).

This plugin is needed to allow Gerrit to use individual users as Groups for being
used in Gerrit ACLs. As of Gerrit 3.3 singleuserplugin is a core plugin and
included in Gerrit tree (if it was cloned recursively).

Example:
  cd gerrit
  bazelisk build plugins/singleusergroup
  cp bazel-bin/plugins/singleusergroup/singleusergroup.jar $GERRIT_SITE/plugins/.

### Building GitHub integration for Gerrit

Just clone the Git repository (see https://gerrit-review.googlesource.com/#/admin/projects/plugins/github)
and do a `mvn install` from the root directory.
This will create two JARs under github-oauth/target and github-plugin/target: the oauth is a JAR library
to be copied to $GERRIT_SITE/lib whilst the plugin JAR has to be installed as usual under $GERRIT_SITE/plugins.

Example:
  git clone https://gerrit.googlesource.com/plugins/github
  cd github
  mvn install
  cp github-oauth/target/github-oauth-*.jar $GERRIT_SITE/lib
  cp github-plugin/target/github-plugin-*.jar $GERRIT_SITE/plugins

### Register Gerrit as a Github OAuth application ###

* login to Github
* open the URL: https://github.com/settings/applications/new
* Application name: Gerrit
* Homepage URL: https://review.my-domain.org
* Authorization callback URL: https://review.my-domain.org/oauth

Note: Client ID & Client Secret are generated that used in the next step.

### Running Gerrit init to configure GitHub OAuth

* java -jar bazel-bin/gerrit.war init -d `$GERRIT_SITE`
* User Authentication
* Authentication method []: HTTP
* Get username from custom HTTP header [Y/n]? Y
* Username HTTP header []: GITHUB_USER
* SSO logout URL : /oauth/reset

* GitHub Integration

* GitHub URL: [https://github.com]: <confirm>
* Use GitHub for Gerrit login? [Y/n] Y
* ClientId []: <provided client id from previous step>
* ClientSecret []: <provided client secret from previous step>

### Receiving Pull Request events to automatically import

* Create a github user account which automatic import operation uses.
* Register the account to your gerrit site by logging into Gerrit with the
  account.
* [Create webhook](https://developer.github.com/webhooks/creating/) on your
  github repository.
  * The payload URL should be something like
    http://*your-gerrit-host.example*/plugins/github-plugin-*version*/webhook.
  * It is recommended to specify some webhook secret.
* Edit `etc/gerrit.config` and `etc/secure.config` files in your `$gerrit_site`.
  * Add the github user account name as `webhookUser` entry in `github` section
    of `etc/gerrit.config`
  * Add the webhook secret as `webhookSecret` entry in `github` section of
    `etc/secure.config`.

### Enhancing GitHub Sign-In Redirection in a Multi-Gerrit Setup

For some context, it's important to note that as part of the GitHub sign-in
process, a redirection is initiated to GitHub, which is commonly referred to as
the [user's GitHub identity request](https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/authorizing-oauth-apps#1-request-a-users-github-identity).
Within this process, a key query parameter known as [redirect_uri](https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/authorizing-oauth-apps#redirect-urls)
plays a pivotal role, and it's defined as follows:

```
The redirect_uri parameter is optional. If omitted, GitHub will redirect users
to the callback URL configured in the OAuth app settings. If provided, the
redirect URL's host (excluding sub-domains) and port must exactly match the
callback URL. The redirect URL's path must reference a subdirectory of the
callback URL.
```

GitHub utilizes this query parameter to manage the redirection of traffic once
authorization has been granted.

Now, in the context of a "Gerrit multi-setup topology," where a primary domain,
namely `example.com`, functions as a geo-location load balancer, alongside
multiple Gerrit sites like `review-1.example.com` and `review-2.example.com`, a
noteworthy scenario arises. Previously, when the sign-in process was initiated
from any of these multiple Gerrit sites, the construction of the `redirect_uri`
query parameter relied on information sourced from the `gerrit.canonicalWebUrl`
property, as specified in the [etc/gerrit.config file](https://gerrit-review.googlesource.com/Documentation/config-gerrit.html).
It's important to emphasize that this property occasionally contained a URL
with a host that matched the primary domain, i.e., `example.com`. Consequently,
when users attempted to sign in via GitHub, they were redirected to this
primary domain, leading to an unsatisfactory user experience.

To address this issue and ensure that the redirection correctly points to the
Gerrit site that initiated the sign-in flow (e.g., `review-1.example.com`) rather
than the `example.com` domain defined in the `gerrit.canonicalWebUrl` property,
it becomes necessary to pass along the [X-Forwarded-Host](https://www.rfc-editor.org/rfc/rfc7239.html)
from the upstream proxy.

### Contributing to the GitHub plugin

The GitHub plugin uses the lombok library, which provides a set of
annotations to reduce the amount of boilerplate code that has to be
written.

To build the plugin in Eclipse, the Lombok Eclipse integration must be
installed.

Download lombok.jar from http://projectlombok.org/ and install:


```
  java -jar lombok.jar
```

Follow the instructions to select your Eclipse installation if the
installer cannot detect it automatically.

After the installation, Eclipse must be restarted and compilation
errors should disappear.

### Notes

#### Magic refs

Before importing a repository from github, this plugin checks that its git refs
do not clash with Gerrit magic refs, since importing those refs would prevent
users from creating change requests.

Attempting to import repositories having refs starting with `refs/for/` or
`refs/meta` will fail with an error message.
For example:

```text
Found 2 ref(s): Please remove or rename the following refs and try again:
  refs/for/foo, refs/meta/bar
```

More information on Gerrit magic refs can be found [here](https://gerrit-review.googlesource.com/Documentation/intro-user.html#upload-change)