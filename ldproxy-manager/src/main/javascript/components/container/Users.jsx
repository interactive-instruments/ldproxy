
import React, { Component } from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux'
import { connectRequest, mutateAsync } from 'redux-query';
import { push } from 'redux-little-router'

import UserApi from '../../apis/UserApi'

@connect(
    (state, props) => {
        return {
            users: state.entities.users,
            userIds: state.entities.userIds,
            user: state.entities.users && props.urlParams ? state.entities.users[props.urlParams._] : null
        }
    },
    (dispatch) => {
        return {
            showUser: (id) => {
                dispatch(push(`/users/${id}`))
            },
            deleteUser: (id) => {
                dispatch(mutateAsync(UserApi.deleteUserQuery(id)));
            }
        }
    }
)

@connectRequest(
    (props) => {
        if (!props.userIds) {
            return UserApi.getUsersQuery()
        }
        return props.userIds.map(id => UserApi.getUserQuery(id))
    })

export default class Users extends Component {

    render() {
        const { users, userIds, user, showUser, deleteUser, children, ...rest } = this.props;

        const componentProps = {
            users,
            userIds,
            user,
            showUser,
            deleteUser
        }

        const childrenWithProps = React.Children.map(children,
            (child) => React.cloneElement(child, {}, React.cloneElement(React.Children.only(child.props.children), componentProps))
        );

        return <div>
            {childrenWithProps}
        </div>
    }
}

Users.propTypes = {
    //children: PropTypes.element.isRequired
};

Users.defaultProps = {
};
