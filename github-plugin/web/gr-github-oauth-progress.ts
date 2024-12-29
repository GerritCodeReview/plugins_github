import { PluginApi } from '@gerritcodereview/typescript-api/plugin';
import { AuthInfo } from '@gerritcodereview/typescript-api/rest-api';
import { CSSResult, LitElement, css, html } from "lit";
import { customElement, property, query, state } from 'lit/decorators.js';

declare global {
    interface Window {
        CANONICAL_PATH:string;
    }
}

function getBaseUrl(): string {
  // window is not defined in service worker, therefore no CANONICAL_PATH
  if (typeof window === 'undefined')
    return '';
  return self.CANONICAL_PATH || '';
}

@customElement('gr-github-oauth-progress')
export class GrGitHubOAuthProgress extends LitElement {
    @query('#gitHubOAuthProgress')
    gitHubOAuthProgress?: HTMLDialogElement;

    @property() plugin!: PluginApi;

    @state() authInfo?: AuthInfo;

    @state() loggedIn?: boolean;

    @state() currentNavigationPath?: string;

    override connectedCallback() {
        super.connectedCallback();
        const restApi = this.plugin.restApi();
        if (!this.authInfo) {
            restApi.getConfig().then(config => this.authInfo = config?.auth);
        }
        restApi.getLoggedIn().then(loggedIn => this.loggedIn = loggedIn);
        document.addEventListener(
          'nav-report',
          // the `nav-report` event is emitted before `window.location` is updated, in order
          // to get the current location, we need to delay `this.updateLocationPath` execution
          // by putting it on the end of processing queue
          () => setTimeout(() => this.updateLocationPath(), 0));
        this.updateLocationPath();
    }

    static override get styles() {
        return [
            window.Gerrit.styles.spinner as CSSResult,
            window.Gerrit.styles.font as CSSResult,
            window.Gerrit.styles.modal as CSSResult,
            css`
            .loginButton {
                --gr-button-text-color: var(--header-text-color);
                color: var(--header-text-color);
                padding: var(--spacing-m) var(--spacing-l);
            }
            .loadingContainer {
                display: flex;
                gap: var(--spacing-s);
                align-items: baseline;
                padding: var(--spacing-xxl);
            }
            .loadingSpin {
                vertical-align: top;
                position: relative;
                top: 3px;
            }
          `];
    }

    override render() {
        if (!this.authInfo || this.loggedIn !== false) {
            return;
        }
        let login_url = this.authInfo.login_url;
        if (login_url == null) {
          login_url = getBaseUrl();
        } else {
          login_url = getBaseUrl() + login_url;
        }
        const loginWithRedirect = new URL(login_url, window.location.origin + "/" + getBaseUrl());
        if (this.currentNavigationPath) {
          loginWithRedirect.pathname = loginWithRedirect.pathname + this.currentNavigationPath.slice(getBaseUrl().length);
        }

        return html`
            <a class="loginButton" href="${loginWithRedirect}" @click=${this.showModal}>
                ${this.authInfo.login_text}
            </a>
            <dialog id="gitHubOAuthProgress">
                <div class="loadingContainer">
                    <span class="loadingSpin"></span>
                    <span class="loadingText">Waiting for GitHub API response ...</span>
                </div>
           </dialog>
        `
    }

    private updateLocationPath() {
        this.currentNavigationPath = window.location.pathname;
    }

    private showModal() {
        setTimeout(() => this.gitHubOAuthProgress?.showModal(), 550);
    }
}
