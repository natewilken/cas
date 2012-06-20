select passwordStateFlag, passwordExpirationDate, lastPasswordChangeDate
from Principal
where principal = :principal