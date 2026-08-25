import { useState } from 'react'
import { changeUserRole, getUsers } from '../api.js'
import Pager from './Pager.jsx'
import { useAsyncData } from './useAsyncData.js'

export default function UsersTab({ myId }) {
    const [role, setRole] = useState('')
    const [pageNumber, setPageNumber] = useState(0)

    const {
        data: page,
        error,
        loading,
        reload,
        fail,
    } = useAsyncData(() => getUsers(role, pageNumber), [role, pageNumber])

    async function handleRoleChange(user, next) {
        const message =
            next === 'ADMIN'
                ? `${user.nickname} 님에게 관리자 권한을 줍니다. 계속할까요?`
                : `${user.nickname} 님의 관리자 권한을 해제합니다. 계속할까요?`

        if (!window.confirm(message)) {
            return
        }

        try {
            await changeUserRole(user.id, next)
            reload()
        } catch (e) {
            fail(e)
        }
    }

    return (
        <div className="admin__panel">
            <div className="admin__toolbar">
                <label htmlFor="user-role">권한</label>

                <select
                    id="user-role"
                    value={role}
                    onChange={(e) => {
                        setRole(e.target.value)
                        setPageNumber(0)
                    }}
                >
                    <option value="">전체</option>
                    <option value="ADMIN">관리자</option>
                    <option value="USER">일반</option>
                </select>

                <button onClick={reload} disabled={loading}>
                    새로고침
                </button>
            </div>

            {error && <p className="admin__error">{error}</p>}

            {loading && <p className="admin__empty">불러오는 중…</p>}

            {!loading && page?.content.length === 0 && (
                <p className="admin__empty">해당하는 사용자가 없습니다.</p>
            )}

            {!loading && page?.content.length > 0 && (
                <div className="admin__scroll">
                    <table>
                        <thead>
                            <tr>
                                <th>번호</th>
                                <th>이메일</th>
                                <th>닉네임</th>
                                <th>권한</th>
                                <th>가입일</th>
                                <th>변경</th>
                            </tr>
                        </thead>

                        <tbody>
                            {page.content.map((user) => (
                                <tr key={user.id}>
                                    <td>{user.id}</td>
                                    <td>{user.email}</td>
                                    <td>{user.nickname}</td>
                                    <td>
                                        {user.role === 'ADMIN' ? (
                                            <span className="admin__badge admin__badge--admin">
                                                관리자
                                            </span>
                                        ) : (
                                            '일반'
                                        )}
                                    </td>
                                    <td>{formatDate(user.createdAt)}</td>
                                    <td>
                                        {user.id === myId ? (
                                            // 스스로 권한을 내리면 되돌릴 수단이 없어 서버가 막는다.
                                            <span className="admin__who">본인</span>
                                        ) : user.role === 'ADMIN' ? (
                                            <button onClick={() => handleRoleChange(user, 'USER')}>
                                                관리자 해제
                                            </button>
                                        ) : (
                                            <button onClick={() => handleRoleChange(user, 'ADMIN')}>
                                                관리자로
                                            </button>
                                        )}
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}

            <Pager page={page} onChange={setPageNumber} />
        </div>
    )
}

function formatDate(value) {
    return value ? value.slice(0, 10) : ''
}
