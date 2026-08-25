import { useState } from 'react'
import {
    createComment,
    createReport,
    deleteComment,
    deleteStream,
    getComments,
    getStream,
    toggleLike,
} from '../api.js'
import { useAuth } from '../useAuth.js'
import Pager from '../components/Pager.jsx'
import { assetUrl } from '../assets.js'
import { navigate } from '../router.js'
import Link from '../components/Link.jsx'
import { useAsyncData } from '../useAsyncData.js'

export default function StreamPage({ id }) {
    const { me } = useAuth()
    const [commentPage, setCommentPage] = useState(0)
    const [content, setContent] = useState('')

    const {
        data: stream,
        error,
        loading,
        reload,
        fail,
    } = useAsyncData(() => getStream(id), [id])

    const {
        data: comments,
        reload: reloadComments,
        fail: failComments,
    } = useAsyncData(() => getComments(id, commentPage), [id, commentPage])

    async function handleLike() {
        try {
            await toggleLike(id)
            reload()
        } catch (e) {
            fail(e)
        }
    }

    async function handleComment(event) {
        event.preventDefault()

        try {
            await createComment(id, content.trim())
            setContent('')
            reloadComments()
            reload()
        } catch (e) {
            failComments(e)
        }
    }

    async function handleDeleteComment(commentId) {
        if (!window.confirm('댓글을 삭제할까요?')) return

        try {
            await deleteComment(id, commentId)
            reloadComments()
            reload()
        } catch (e) {
            failComments(e)
        }
    }

    async function handleReport() {
        const reason = window.prompt('신고 사유를 입력해 주세요.')

        if (!reason?.trim()) return

        try {
            await createReport('STREAM', Number(id), reason.trim())
            window.alert('신고가 접수되었습니다.')
        } catch (e) {
            fail(e)
        }
    }

    async function handleDelete() {
        if (!window.confirm('이 영상을 삭제할까요?')) return

        try {
            await deleteStream(id)
            navigate({ view: 'home' })
        } catch (e) {
            fail(e)
        }
    }

    if (loading) return <p className="empty">불러오는 중…</p>
    if (error) return <p className="error">{error}</p>
    if (!stream) return null

    const mine = me?.id === stream.userId

    return (
        <section>
            <video className="player" controls src={assetUrl(stream.videoUrl)} />

            <h2>{stream.title}</h2>

            <p className="meta">
                <Link to={{ view: 'channel', id: stream.userId }}>{stream.nickname}</Link>
                {' · '}조회 {stream.viewCount}
                {stream.categoryName && ` · ${stream.categoryName}`}
            </p>

            <div className="toolbar">
                <button onClick={handleLike} disabled={!me}>
                    {stream.likedByMe ? '♥' : '♡'} 좋아요 {stream.likeCount}
                </button>

                {me && !mine && <button onClick={handleReport}>신고</button>}

                {mine && (
                    <>
                        <button onClick={() => navigate({ view: 'upload', id: stream.id })}>
                            수정
                        </button>
                        <button onClick={handleDelete}>삭제</button>
                    </>
                )}
            </div>

            {stream.description && <p className="description">{stream.description}</p>}

            <h3>댓글 {stream.commentCount}</h3>

            {me ? (
                <form className="toolbar" onSubmit={handleComment}>
                    <input
                        value={content}
                        onChange={(e) => setContent(e.target.value)}
                        placeholder="댓글을 입력하세요"
                        required
                    />
                    <button type="submit">등록</button>
                </form>
            ) : (
                <p className="meta">댓글을 쓰려면 로그인이 필요합니다.</p>
            )}

            {comments?.content.length === 0 && <p className="empty">첫 댓글을 남겨보세요.</p>}

            <ul className="comments">
                {comments?.content.map((comment) => (
                    <li key={comment.id}>
                        <strong>{comment.nickname}</strong>{' '}
                        <span className="meta">{formatDateTime(comment.createdAt)}</span>
                        <p>{comment.content}</p>

                        {me?.nickname === comment.nickname && (
                            <button onClick={() => handleDeleteComment(comment.id)}>삭제</button>
                        )}
                    </li>
                ))}
            </ul>

            <Pager page={comments} onChange={setCommentPage} />
        </section>
    )
}

function formatDateTime(value) {
    return value ? value.replace('T', ' ').slice(0, 16) : ''
}
