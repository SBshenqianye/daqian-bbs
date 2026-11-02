INSERT INTO bbs_schema_version (version, description, applied_at)
VALUES ('v039-moderator-role', '版主账号角色（user_type=4）', '2026-11-02 00:00:00')
ON CONFLICT (version) DO UPDATE
  SET description = EXCLUDED.description,
      applied_at  = EXCLUDED.applied_at;
