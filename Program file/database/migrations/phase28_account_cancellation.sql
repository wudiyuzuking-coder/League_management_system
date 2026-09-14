USE league_ticket;

-- Schema-only extension for account soft cancellation. Existing accounts and
-- every historical business reference remain unchanged.
ALTER TABLE sys_user
    DROP CHECK ck_sys_user_status,
    ADD CONSTRAINT ck_sys_user_status CHECK (
        user_status IN (
            'PENDING_ACTIVATION',
            'PENDING_CLUB_APPROVAL',
            'ENABLED',
            'DISABLED',
            'LOCKED',
            'CANCELLED'
        )
    );
