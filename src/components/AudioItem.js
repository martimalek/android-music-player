import React, { useRef } from 'react';
import { StyleSheet, Text, TouchableOpacity, Animated, PanResponder, View, Dimensions } from 'react-native';
import PropTypes from 'prop-types';
import { Colors } from '../styles';

export const AudioItem = ({ title, onSwipeRight, isSelected, isInSelectedQueue, ...props }) => {
    const pan = useRef(new Animated.ValueXY()).current;
    const isPannable = useRef(true);

    const handleSwipeRight = () => {
        isPannable.current = false;
        onSwipeRight();
    };

    console.log('Hi')

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
            style={{ transform: [{ translateX: pan.x }], ...(isSelected ? styles.selected : {}) }}
            {...panResponder.panHandlers}
        >
            <TouchableOpacity style={{ ...styles.item }} {...props}>
                <Text style={styles.itemText}>{title}</Text>
                {isInSelectedQueue && (<View style={styles.selectedQueueItem} />)}
            </TouchableOpacity>
        </Animated.View>
    );
};

AudioItem.propTypes = {
    title: PropTypes.string.isRequired,
    onSwipeRight: PropTypes.func.isRequired,
    isInSelectedQueue: PropTypes.bool.isRequired,
    isSelected: PropTypes.bool,
};

const styles = StyleSheet.create({
    item: {
        paddingVertical: 20,
        marginVertical: 8,
        marginHorizontal: 16,
        borderRadius: 10,
        flex: 1,
        justifyContent: 'space-between',
        alignItems: 'center',
        flexDirection: 'row',
        width: Dimensions.get('window').width - 50,
    },
    selected: {
        backgroundColor: Colors.selected,
        borderRadius: 10,
    },
    selectedQueueItem: {
        backgroundColor: Colors.orange,
        height: 10,
        width: 30,
        bottom: 0,
        right: 0,
        borderBottomRightRadius: 10,
        borderTopLeftRadius: 10,
    },
    itemText: {
        color: 'white',
        paddingHorizontal: 20,
        maxWidth: Dimensions.get('window').width - 90,
        maxHeight: 20,
        overflow: 'hidden'
    },
});
