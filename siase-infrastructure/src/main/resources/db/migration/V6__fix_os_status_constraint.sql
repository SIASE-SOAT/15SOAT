-- =====================================================
-- SIASE - Corrige constraint duplicada de status da OS
-- O constraint 'ordens_de_servico_status_check' é um
-- legado com nome gerado automaticamente que não foi
-- removido pela V5. Ele não inclui 'CANCELADA'.
-- =====================================================

ALTER TABLE ordens_de_servico DROP CONSTRAINT IF EXISTS ordens_de_servico_status_check;
