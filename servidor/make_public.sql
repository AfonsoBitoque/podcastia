-- Make all podcasts public so they appear in community/trending
UPDATE podcasts SET publico = true WHERE publico = false;
