import '@gerritcodereview/typescript-api/gerrit';
import './gr-github-oauth-progress';

window.Gerrit.install(plugin => {
    plugin.registerCustomComponent(
        'auth-link',
        'gr-github-oauth-progress',
        { replace: true });
});
