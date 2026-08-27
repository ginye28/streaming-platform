package com.sp.api.block.service;

import com.sp.api.block.dto.BlockResponse;
import com.sp.api.block.dto.BlockedUserResponse;
import com.sp.api.block.entity.Block;
import com.sp.api.block.repository.BlockRepository;
import com.sp.api.common.exception.BadRequestException;
import com.sp.api.common.exception.NotFoundException;
import com.sp.api.common.response.PageResponse;
import com.sp.api.user.entity.User;
import com.sp.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlockService {

    /**
     * 차단한 사용자가 없을 때 채워 넣는 값. 실제 사용자 id 는 1 이상이라 절대 겹치지 않는다.
     * JPQL 의 "not in" 은 빈 컬렉션을 받을 수 없어서, 피드 조회 쪽에 항상 비어 있지 않은
     * 컬렉션을 넘겨주기 위해 필요하다.
     */
    private static final Long NO_BLOCK_SENTINEL = -1L;

    private final BlockRepository blockRepository;
    private final UserRepository userRepository;

    @Transactional
    public BlockResponse toggle(Long targetUserId, String email) {

        User blocker = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        User blocked = userRepository.findById(targetUserId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        if (blocker.getId().equals(blocked.getId())) {
            throw new BadRequestException("자기 자신은 차단할 수 없습니다.");
        }

        boolean nowBlocked = blockRepository
                .findByBlockerIdAndBlockedId(blocker.getId(), blocked.getId())
                .map(block -> {
                    blockRepository.delete(block);
                    return false;
                })
                .orElseGet(() -> {
                    blockRepository.save(new Block(blocker, blocked));
                    return true;
                });

        return new BlockResponse(nowBlocked);
    }

    public PageResponse<BlockedUserResponse> findMyBlocks(String email, Pageable pageable) {

        User blocker = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        return PageResponse.from(
                blockRepository.findByBlockerId(blocker.getId(), pageable)
                        .map(block -> BlockedUserResponse.from(block.getBlocked()))
        );
    }

    /**
     * 피드에서 걸러낼 사용자 id 목록. 비로그인이거나 차단한 사람이 없으면
     * {@link #NO_BLOCK_SENTINEL} 하나만 담긴 목록을 돌려준다.
     */
    public List<Long> excludedUserIds(String viewerEmail) {

        if (viewerEmail == null) {
            return List.of(NO_BLOCK_SENTINEL);
        }

        List<Long> blocked = userRepository.findByEmail(viewerEmail)
                .map(user -> blockRepository.findBlockedUserIdsByBlockerId(user.getId()))
                .orElseGet(List::of);

        return blocked.isEmpty() ? List.of(NO_BLOCK_SENTINEL) : blocked;
    }
}
