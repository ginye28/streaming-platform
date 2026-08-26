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
    const [replyTo, setReplyTo] = useState(null)
    const [replyContent, setReplyContent] = useState('')

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

    async function handleReply(event, parentId) {
        event.preventDefault()

        try {
            await createComment(id, replyContent.trim(), parentId)
            closeReply()
            reloadComments()
            reload()
        } catch (e) {
            failComments(e)
        }
    }

    function openReply(commentId) {
        setReplyTo(commentId)
        setReplyContent('')
    }

    function closeReply() {
        setReplyTo(null)
        setReplyContent('')
    }

    async function handleDeleteComment(comment) {
        const message = comment.replies?.length
            ? `답글 ${comment.replies.length}개도 함께 삭제됩니다. 삭제할까요?`
            : '댓글을 삭제할까요?'

        if (!window.confirm(message)) return

        try {
            await deleteComment(id, comment.id)
            if (replyTo === comment.id) closeReply()
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
                        <CommentLine
                            comment={comment}
                            me={me}
                            onReply={() => openReply(comment.id)}
                            onDelete={() => handleDeleteComment(comment)}
                        />

                        {comment.replies?.length > 0 && (
                            <ul className="comments comments--replies">
                                {comment.replies.map((reply) => (
                                    <li key={reply.id}>
                                        <CommentLine
                                            comment={reply}
                                            me={me}
                                            onDelete={() => handleDeleteComment(reply)}
                                        />
                                    </li>
                                ))}
                            </ul>
                        )}

                        {replyTo === comment.id && (
                            <form
                                className="comment__reply"
                                onSubmit={(e) => handleReply(e, comment.id)}
                            >
                                <input
                                    value={replyContent}
                                    onChange={(e) => setReplyContent(e.target.value)}
                                    placeholder={`${comment.nickname} 님에게 답글`}
                                    required
                                    autoFocus
                                />
                                <button type="submit">등록</button>
                                <button type="button" onClick={closeReply}>
                                    취소
                                </button>
                            </form>
                        )}
                    </li>
                ))}
            </ul>

            <Pager page={comments} onChange={setCommentPage} />
        </section>
    )
}

/** 댓글 한 줄. onReply 가 없으면 답글 버튼도 없다 — 답글에는 다시 답글을 달 수 없다. */
function CommentLine({ comment, me, onReply, onDelete }) {
    const canReply = Boolean(me && onReply)
    const canDelete = Boolean(me) && me.nickname === comment.nickname

    return (
        <>
            <strong>{comment.nickname}</strong>{' '}
            <span className="meta">{formatDateTime(comment.createdAt)}</span>
            <p>{comment.content}</p>

            {(canReply || canDelete) && (
                <div className="comment__actions">
                    {canReply && <button onClick={onReply}>답글</button>}
                    {canDelete && <button onClick={onDelete}>삭제</button>}
                </div>
            )}
        </>
    )
}

function formatDateTime(value) {
    return value ? value.replace('T', ' ').slice(0, 16) : ''
}
