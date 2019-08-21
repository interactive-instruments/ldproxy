import React from 'react';
import { Box } from 'grommet';

export default props => {
    return (
        <Box fill={true}>
            <iframe src='../system/console' style={{ width: '100%', height: '100%', border: '0' }} />
        </Box>
    )
}
