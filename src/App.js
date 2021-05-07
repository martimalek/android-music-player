import 'react-native-gesture-handler';
import React, { useEffect, useState, useRef } from 'react';
import PropTypes from 'prop-types';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { Animated, Dimensions, StyleSheet } from "react-native";
import BootSplash from "react-native-bootsplash";

import { MainNavigator } from './navigation/MainNavigator';
const bootSplashLogo = require("./assets/images/bootsplash_logo.png");

const App = () => {
    const [splashIsVisible, setSplashIsVisible] = useState(true);
    const [logoIsLoaded, setLogoIsLoaded] = useState(false);
    const opacity = useRef(new Animated.Value(1));
    const translateY = useRef(new Animated.Value(0));

    const init = async () => {
        await BootSplash.hide();

        Animated.stagger(250, [
            Animated.spring(translateY.current, {
                useNativeDriver: true,
                toValue: -50,
            }),
            Animated.spring(translateY.current, {
                useNativeDriver: true,
                toValue: Dimensions.get("window").height,
            }),
        ]).start();

        Animated.timing(opacity.current, {
            useNativeDriver: true,
            toValue: 0,
            duration: 150,
            delay: 350,
        }).start(() => { setSplashIsVisible(false) });
    };

    useEffect(() => {
        logoIsLoaded && init();
    }, [logoIsLoaded]);

    return (
        <MainProvider>
            <MainNavigator />
            {splashIsVisible && (
                <Animated.View
                    style={[
                        StyleSheet.absoluteFill,
                        styles.bootsplash,
                        { opacity: opacity.current },
                    ]}
                >
                    <Animated.Image
                        source={bootSplashLogo}
                        fadeDuration={0}
                        onLoadEnd={() => setLogoIsLoaded(true)}
                        style={[
                            styles.logo,
                            { transform: [{ translateY: translateY.current }] },
                        ]}
                    />
                </Animated.View>
            )}
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

const styles = StyleSheet.create({
    bootsplash: {
        flex: 1,
        justifyContent: "center",
        alignItems: "center",
    },
    logo: {
        height: 100,
        width: 100,
    },
});