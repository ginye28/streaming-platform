import { useEffect, useState } from 'react'
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
import ChannelSubscribe from '../components/ChannelSubscribe.jsx'
import FilePlayer from '../components/FilePlayer.jsx'
import MoreStreams from '../components/MoreStreams.jsx'
import OshiMark from '../components/OshiMark.jsx'
import Pager from '../components/Pager.jsx'
import { assetUrl } from '../assets.js'
import { loginTo, navigate } from '../router.js'
import { timeAgo } from '../time.js'
import { usePageTitle } from '../usePageTitle.js'
import { useSlow } from '../useSlow.js'
import Link from '../components/Link.jsx'
import { useAsyncData } from '../useAsyncData.js'

export default function StreamPage({ id }) {
    const { me } = useAuth()
    const [commentPage, setCommentPage] = useState(0)
    const [content, setContent] = useState('')
    const [replyTo, setReplyTo] = useState(null)
    const [replyContent, setReplyContent] = useState('')
    const [panel, setPanel] = useState(null)
    const [reason, setReason] = useState('')
    const [notice, setNotice] = useState(null)
    const [deletingComment, setDeletingComment] = useState(null)
    // 좋아요·신고·삭제가 실패해도 영상과 댓글은 그대로 두고, 그 자리에서 알린다.
    const [actionError, setActionError] = useState(null)
    // 답글이 많은 댓글은 접어 두고, 펼친 댓글의 id 를 기억한다.
    const [openReplies, setOpenReplies] = useState([])
    const [posting, setPosting] = useState(false)
    // 댓글·답글 등록이 실패했을 때. 입력한 내용은 그대로 두고 그 자리에서 알린다.
    const [postError, setPostError] = useState(null)

    // 신고 접수 같은 안내는 몇 초 뒤에 저절로 사라진다.
    useEffect(() => {
        if (!notice) return

        const timer = setTimeout(() => setNotice(null), 5000)

        return () => clearTimeout(timer)
    }, [notice])

    const {
        data: stream,
        error,
        loading,
        reload,
    } = useAsyncData(() => getStream(id), [id])

    const {
        data: comments,
        error: commentsError,
        reload: reloadComments,
        fail: failComments,
    } = useAsyncData(() => getComments(id, commentPage), [id, commentPage])

    usePageTitle(stream?.title)

    // 무료 서버가 잠들어 있으면 첫 응답이 오래 걸린다. 그 이유를 알려 준다.
    const slow = useSlow(loading)

    async function handleLike() {
        // 좋아요를 누르려는 바로 그 순간이 로그인을 권하기 가장 좋은 때다. 보던 영상으로 돌아오게 보낸다.
        if (!me) {
            navigate(loginTo())
            return
        }

        setActionError(null)

        try {
            await toggleLike(id)
            reload()
        } catch (e) {
            setActionError(`좋아요를 처리하지 못했어요. ${e.message}`)
        }
    }

    async function handleComment(event) {
        event.preventDefault()
        setPosting(true)
        setPostError(null)

        try {
            await createComment(id, content.trim())
            setContent('')
            reloadComments()
            reload()
        } catch (e) {
            setPostError(`댓글을 등록하지 못했어요. 쓴 내용은 그대로예요. ${e.message}`)
        } finally {
            setPosting(false)
        }
    }

    async function handleReply(event, parentId) {
        event.preventDefault()
        setPosting(true)
        setPostError(null)

        try {
            await createComment(id, replyContent.trim(), parentId)
            closeReply()
            reloadComments()
            reload()
        } catch (e) {
            setPostError(`답글을 등록하지 못했어요. 쓴 내용은 그대로예요. ${e.message}`)
        } finally {
            setPosting(false)
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
        setDeletingComment(null)

        try {
            await deleteComment(id, comment.id)
            if (replyTo === comment.id) closeReply()
            reloadComments()
            reload()
        } catch (e) {
            failComments(e)
        }
    }

    function closePanel() {
        setPanel(null)
        setReason('')
    }

    async function handleReport(event) {
        event.preventDefault()

        try {
            await createReport('STREAM', Number(id), reason.trim())
            closePanel()
            setNotice('신고가 접수되었어요.')
        } catch (e) {
            setActionError(`신고를 접수하지 못했어요. ${e.message}`)
        }
    }

    async function handleDelete() {
        try {
            await deleteStream(id)
            navigate({ view: 'home' })
        } catch (e) {
            closePanel()
            setActionError(`영상을 삭제하지 못했어요. ${e.message}`)
        }
    }

    if (loading) {
        return (
            <section className="stream">
                <div className="player-box">
                    <div className="player-box__ghost" />
                    <div className="player-box__wait" role="status">
                        <strong>영상을 불러오는 중</strong>
                        {slow && <span>서버를 깨우는 중이에요. 최대 1분쯤 걸려요</span>}
                        <span className="player-box__dots" aria-hidden="true">
                            <i />
                            <i />
                            <i />
                        </span>
                    </div>
                </div>
            </section>
        )
    }

    // 영상 정보 자체를 못 받았을 때만 페이지 전체를 오류로 바꾼다.
    if (error && !stream) {
        return (
            <p className="error" role="alert">
                {error} <button type="button" onClick={reload}>다시 시도</button>
            </p>
        )
    }
    if (!stream) return null

    const mine = me?.id === stream.userId

    return (
        <section className="stream">
          <div className="stream__main">
            <FilePlayer
                src={assetUrl(stream.videoUrl)}
                poster={assetUrl(stream.thumbnailUrl)}
                label={`${stream.title} 영상`}
            />

            <div className="stream__info">
                <h1>{stream.title}</h1>

                <p className="meta">
                    조회 {stream.viewCount?.toLocaleString('ko-KR')}
                    {stream.createdAt && (
                        <>
                            {' · '}
                            <time dateTime={stream.createdAt} title={formatDateTime(stream.createdAt)}>
                                {timeAgo(stream.createdAt)}
                            </time>
                        </>
                    )}
                    {stream.categoryName && ` · ${stream.categoryName}`}
                </p>

                <ChannelSubscribe channelId={stream.userId} identity="named" />

                <div className="stream__actions">
                    <button
                        type="button"
                        className="like"
                        aria-pressed={Boolean(stream.likedByMe)}
                        onClick={handleLike}
                        title={me ? undefined : '누르면 로그인 화면으로 가요'}
                    >
                        <svg width="18" height="18" viewBox="0 0 24 24" aria-hidden="true">
                            <path d="M12 20s-7.5-4.6-7.5-10.2A4.3 4.3 0 0 1 12 7.2a4.3 4.3 0 0 1 7.5 2.6C19.5 15.4 12 20 12 20z" />
                        </svg>
                        좋아요 {stream.likeCount?.toLocaleString('ko-KR')}
                    </button>

                    {!me && <span className="meta like__hint">누르면 로그인해요</span>}

                    <div className="stream__manage">
                        {me && !mine && (
                            <button type="button" onClick={() => setPanel('report')}>
                                신고
                            </button>
                        )}

                        {mine && (
                            <>
                                <button type="button" onClick={() => navigate({ view: 'upload', id: stream.id })}>
                                    수정
                                </button>
                                <button type="button" onClick={() => setPanel('delete')}>
                                    삭제
                                </button>
                            </>
                        )}
                    </div>
                </div>

                {notice && (
                    <p className="meta" role="status">
                        {notice}
                    </p>
                )}

                {actionError && (
                    <p className="error" role="alert">
                        {actionError}{' '}
                        <button type="button" onClick={() => setActionError(null)}>
                            닫기
                        </button>
                    </p>
                )}

                {panel === 'report' && (
                    <form className="stream__panel" onSubmit={handleReport}>
                        <label>
                            신고 사유
                            <input
                                value={reason}
                                onChange={(e) => setReason(e.target.value)}
                                placeholder="어떤 점이 문제인가요?"
                                maxLength={200}
                                required
                                autoFocus
                            />
                        </label>
                        <div className="stream__panel-actions">
                            <button type="submit">신고 접수</button>
                            <button type="button" onClick={closePanel}>
                                취소
                            </button>
                        </div>
                    </form>
                )}

                {panel === 'delete' && (
                    <div className="stream__panel" role="group" aria-label="영상 삭제 확인">
                        <p>이 영상을 삭제할까요? 삭제하면 되돌릴 수 없어요.</p>
                        <div className="stream__panel-actions">
                            <button type="button" className="button--danger" onClick={handleDelete}>
                                삭제
                            </button>
                            <button type="button" onClick={closePanel}>
                                취소
                            </button>
                        </div>
                    </div>
                )}

                {stream.description && <Description text={stream.description} />}
            </div>
          </div>

          <div className="stream__comments">
            <h2 className="comments__title">댓글 {stream.commentCount}</h2>

            {commentsError && (
                <p className="error" role="alert">
                    {commentsError} <button type="button" onClick={reloadComments}>다시 시도</button>
                </p>
            )}

            {me ? (
                <form className="toolbar" onSubmit={handleComment}>
                    <input
                        value={content}
                        onChange={(e) => setContent(e.target.value)}
                        placeholder="댓글을 입력하세요"
                        required
                    />
                    <button type="submit" disabled={posting}>
                        {posting ? '등록 중…' : '등록'}
                    </button>
                </form>
            ) : (
                <p className="stream__login">
                    <Link to={loginTo()} className="cta cta--sm cta--soft">
                        로그인하고 댓글 남기기
                    </Link>
                </p>
            )}

            {postError && (
                <p className="error" role="alert">
                    {postError}
                </p>
            )}

            {comments?.content.length === 0 && <p className="empty">첫 댓글을 남겨보세요.</p>}

            <ul className="comments">
                {comments?.content.map((comment) => (
                    <li key={comment.id}>
                        <CommentLine
                            comment={comment}
                            me={me}
                            confirming={deletingComment === comment.id}
                            onReply={() => openReply(comment.id)}
                            onAskDelete={() => setDeletingComment(comment.id)}
                            onCancelDelete={() => setDeletingComment(null)}
                            onDelete={() => handleDeleteComment(comment)}
                        />

                        {comment.replies?.length > 3 && (
                            <button
                                type="button"
                                className="comment__more"
                                aria-expanded={openReplies.includes(comment.id)}
                                onClick={() =>
                                    setOpenReplies((ids) =>
                                        ids.includes(comment.id)
                                            ? ids.filter((value) => value !== comment.id)
                                            : [...ids, comment.id]
                                    )
                                }
                            >
                                {openReplies.includes(comment.id) ? '답글 접기' : `답글 ${comment.replies.length}개 보기`}
                            </button>
                        )}

                        {comment.replies?.length > 0 && (comment.replies.length <= 3 || openReplies.includes(comment.id)) && (
                            <ul className="comments comments--replies">
                                {comment.replies.map((reply) => (
                                    <li key={reply.id}>
                                        <CommentLine
                                            comment={reply}
                                            me={me}
                                            confirming={deletingComment === reply.id}
                                            onAskDelete={() => setDeletingComment(reply.id)}
                                            onCancelDelete={() => setDeletingComment(null)}
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
                                <button type="submit" disabled={posting}>
                                    {posting ? '등록 중…' : '등록'}
                                </button>
                                <button type="button" onClick={closeReply}>
                                    취소
                                </button>
                            </form>
                        )}
                    </li>
                ))}
            </ul>

            <Pager page={comments} onChange={setCommentPage} />
          </div>

            <MoreStreams channelId={stream.userId} currentId={stream.id} nickname={stream.nickname} />
        </section>
    )
}

/** 설명. 길면 세 줄까지만 보이고 "더보기"로 펼친다. */
function Description({ text }) {
    const [open, setOpen] = useState(false)
    const long = text.length > 120 || text.split('\n').length > 3

    return (
        <div className="description-box">
            <p className={`description${long && !open ? ' description--clamped' : ''}`}>{text}</p>

            {long && (
                <button type="button" className="description-box__toggle" aria-expanded={open} onClick={() => setOpen((value) => !value)}>
                    {open ? '접기' : '더보기'}
                </button>
            )}
        </div>
    )
}

/** 댓글 한 줄. onReply 가 없으면 답글 버튼도 없다 — 답글에는 다시 답글을 달 수 없다. */
function CommentLine({ comment, me, confirming, onReply, onAskDelete, onCancelDelete, onDelete }) {
    const canReply = Boolean(me && onReply)
    const canDelete = Boolean(me) && me.nickname === comment.nickname
    const hasReplies = comment.replies?.length > 0

    return (
        <>
            <OshiMark url={comment.oshiMarkUrl} tier={comment.oshiTier} />
            <strong>{comment.nickname}</strong>{' '}
            <time className="meta" dateTime={comment.createdAt} title={formatDateTime(comment.createdAt)}>
                {timeAgo(comment.createdAt)}
            </time>
            <p>{comment.content}</p>

            {confirming ? (
                <div className="comment__actions" role="group" aria-label="댓글 삭제 확인">
                    <span className="meta">
                        {hasReplies ? `답글 ${comment.replies.length}개도 함께 삭제돼요. 삭제할까요?` : '댓글을 삭제할까요?'}
                    </span>
                    <button type="button" className="button--danger" onClick={onDelete}>
                        삭제
                    </button>
                    <button type="button" onClick={onCancelDelete}>
                        취소
                    </button>
                </div>
            ) : (
                (canReply || canDelete) && (
                    <div className="comment__actions">
                        {canReply && (
                            <button type="button" onClick={onReply} aria-label={`${comment.nickname} 님의 댓글에 답글`}>
                                답글
                            </button>
                        )}
                        {canDelete && (
                            <button type="button" onClick={onAskDelete} aria-label={`${comment.nickname} 님의 댓글 삭제`}>
                                삭제
                            </button>
                        )}
                    </div>
                )
            )}
        </>
    )
}

function formatDateTime(value) {
    return value ? value.replace('T', ' ').slice(0, 16) : ''
}
