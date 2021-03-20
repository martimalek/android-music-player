import 'react-native-gesture-handler';
import React, { useEffect } from 'react';
import PropTypes from 'prop-types';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { MainNavigator } from './navigation/MainNavigator';
import { changeNavBarColor } from './services/NavbarColor';
import { Colors } from './styles';

const App = () => {

    useEffect(() => {
        changeNavBarColor(Colors.darkerBackground, Colors.background);
    }, []);

    return (
        <MainProvider>
            <MainNavigator />
        </MainProvider>
    );
};

export default App;

const MainProvider = ({ children }) => (
    <SafeAreaProvider>
        {children}
    </SafeAreaProvider>
);

MainProvider.propTypes = {
    children: PropTypes.node.isRequired,
};
