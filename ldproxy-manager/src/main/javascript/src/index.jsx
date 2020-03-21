import { render } from 'react-dom';
import { launchApp } from 'feature-u';
import { Manager, Theme } from '@xtraplatform/core';
import { Services } from '@xtraplatform/services';

export default launchApp({
    features: [Manager, Theme, Services],
    aspects: [],
    registerRootAppElm: (app) => render(app, document.getElementById('root')),
});
