export const formatDuration = (durationInMillis) => {
    const durationInSeconds = durationInMillis / 1000;

    const minutes = Math.floor(durationInSeconds / 60);
    const seconds = Math.round(durationInSeconds % 60);

    return { minutes, seconds };
};
