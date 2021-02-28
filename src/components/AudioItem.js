import React, { useRef } from 'react';
import { StyleSheet, Text, TouchableOpacity, Animated, PanResponder } from 'react-native';

export const AudioItem = ({ title, style, onSwipeRight, ...props }) => {
    const pan = useRef(new Animated.ValueXY()).current;
    const isPannable = useRef(true);

    const handleSwipeRight = () => {
        isPannable.current = false;
        onSwipeRight();
    };

    const panResponder = useRef(PanResponder.create({
        onMoveShouldSetPanResponder: (evt, gestureState) => {
            const { dx, dy } = gestureState
            return dx > 2 || dx < -2 || dy > 2 || dy < -2
        },
        onPanResponderGrant: () => { pan.setValue({ x: 0, y: 0 }) },
        onPanResponderMove: (e, g) => {
            if (!isPannable.current) return;
            if (g.dx > 0) {
                if (g.dx > 100) handleSwipeRight();
                else {
                    Animated.event(
                        [
                            null,
                            { dx: pan.x._value < 0 ? new Animated.Value(0) : pan.x }
                        ],
                        { useNativeDriver: false },
                    )(e, g);
                }
            }
        },
        onPanResponderRelease: (e, g) => {
            pan.flattenOffset();
            pan.setValue({ x: 0, y: 0 });
            isPannable.current = true;
        },
    })).current;

    return (
        <Animated.View
            style={{ transform: [{ translateX: pan.x }] }}
            {...panResponder.panHandlers}
        >
            <TouchableOpacity style={{ ...styles.item, ...style }} {...props}>
                <Text style={styles.itemText}>{title}</Text>
            </TouchableOpacity>
        </Animated.View>
    );
};

const styles = StyleSheet.create({
    item: {
        backgroundColor: '#597FFB',
        paddingVertical: 20,
        marginVertical: 8,
        marginHorizontal: 16,
        borderRadius: 10,
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
    },
    itemText: {
        color: 'white',
        paddingHorizontal: 20,
    },
});
