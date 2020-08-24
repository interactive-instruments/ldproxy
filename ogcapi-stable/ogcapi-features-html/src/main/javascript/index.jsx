import React from 'react';
import ReactDOM from 'react-dom';
import App from './components/container/App'

const render = (Component) => {
    ReactDOM.render(
        <Component/>,
        document.getElementById('app-wrapper')
    );
};

render(App);

// Hot Module Replacement API
if (module && module.hot) {
    module.hot.accept('./components/container/App', () => {
        render(App)
    });
}
