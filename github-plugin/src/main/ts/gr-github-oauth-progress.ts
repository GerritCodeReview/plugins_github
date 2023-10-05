import { PluginApi } from '@gerritcodereview/typescript-api/plugin';
import { AuthInfo } from '@gerritcodereview/typescript-api/rest-api';
import { CSSResult, LitElement, css, html } from "lit";
import { customElement, property, query, state } from 'lit/decorators.js';

@customElement('gr-github-oauth-progress')
export class GrGitHubOAuthProgress extends LitElement {
    @query('#gitHubOAuthProgress')
    gitHubOAuthProgress?: HTMLDialogElement;

    @property() plugin!: PluginApi;

    @state() authInfo?: AuthInfo

    @state() loggedIn?: boolean

    override connectedCallback() {
        super.connectedCallback();
        const restApi = this.plugin.restApi();
        if (!this.authInfo) {
            restApi.getConfig().then(config => this.authInfo = config?.auth);
        }
        restApi.getLoggedIn().then(loggedIn => this.loggedIn = loggedIn);
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
            return
        }

        return html`
            <a class="loginButton" href=${this.authInfo.login_url} @click=${this.showModal}>
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

    private showModal() {
        setTimeout(() => this.gitHubOAuthProgress?.showModal(), 550);
    }
}
