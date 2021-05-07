import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createStackNavigator } from '@react-navigation/stack';
import { AudioList } from '../screens/AudioList';

const Stack = createStackNavigator();

export const MainNavigator = () => (
    <NavigationContainer>
        <Stack.Navigator headerMode="none">
            <Stack.Screen name="Home" component={AudioList} />
        </Stack.Navigator>
    </NavigationContainer>
);