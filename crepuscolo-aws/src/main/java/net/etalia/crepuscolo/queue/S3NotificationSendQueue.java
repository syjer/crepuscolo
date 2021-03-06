package net.etalia.crepuscolo.queue;

import net.etalia.crepuscolo.domain.BaseEntity;
import net.etalia.crepuscolo.domain.ChangeNotification;
import net.etalia.crepuscolo.domain.ChangeNotification.Type;
import net.etalia.crepuscolo.services.CreationServiceImpl;

public class S3NotificationSendQueue<T> extends S3SendQueue<T> {

	@Override
	public String[] findFieldsFor(Object o, Class<?> clazz) {
		if (!(o instanceof ChangeNotification)) return super.findFieldsFor(o, clazz);
		ChangeNotification<?> notification = (ChangeNotification<?>) o;
		String[] fields = super.findFieldsFor(notification.getInstance(), notification.getClazz());
		fields[0] = ChangeNotification.class.getName();
		fields[1] = "type,clazz,instance,id,created,instance." + fields[1].replaceAll(",", ",instance.");
		return fields;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected String getPayload(T o) {
		if (!(o instanceof ChangeNotification)) return super.getPayload(o);
		try {
			return super.getPayload(o);
		} catch (Throwable e) {
			ChangeNotification<? extends BaseEntity> notification = (ChangeNotification<? extends BaseEntity>) o;
			if (notification.getType().equals(ChangeNotification.Type.DELETED)) {
				BaseEntity instance = notification.getInstance();
				BaseEntity ninst = new CreationServiceImpl().getEmptyInstance(instance.getId());
				return super.getPayload((T)(new ChangeNotification(Type.DELETED, notification.getClazz(), ninst)));
			} else throw e;
		}
	}

	@Override
	@SuppressWarnings("rawtypes")
	protected String getName(Object o) {
		if (!(o instanceof ChangeNotification)) return super.getName(o);
		return super.getName(((ChangeNotification)o).getInstance());
	}

}
