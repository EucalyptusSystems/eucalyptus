package com.eucalyptus.configurable;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.log4j.Logger;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import edu.ucsb.eucalyptus.msgs.ComponentProperty;

public class PropertyDirectory {
  private static final Logger                                 LOG                = Logger.getLogger( PropertyDirectory.class );
  private static final ReadWriteLock                          fqLock             = new ReentrantReadWriteLock();
  private static final Map<String, ConfigurableProperty>      fqMap              = Maps.newHashMap( );
  private static final Multimap<String, ConfigurableProperty> fqPrefixMap        = TreeMultimap.create( );
  private static final Map<String, ConfigurableProperty>      fqPendingMap       = Maps.newHashMap( );
  private static final Multimap<String, ConfigurableProperty> fqPendingPrefixMap = HashMultimap.create( );
  
  private static final List<ConfigurablePropertyBuilder>      builders           = ImmutableList.of(
      new StaticPropertyEntry.StaticPropertyBuilder(),
      new SingletonDatabasePropertyEntry.DatabasePropertyBuilder(),
      new MultiDatabasePropertyEntry.DatabasePropertyBuilder()); //FIXME: make this dynamic kkthx.
                                                                                                                                                              
  public static class NoopEventListener implements PropertyChangeListener {
    public static NoopEventListener NOOP = new NoopEventListener( );
    
    @Override
    public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {}
  }
  
  @SuppressWarnings( { "unchecked" } )
  public static ConfigurableProperty buildPropertyEntry( Class c, Field field ) {
    fqLock.writeLock().lock();
    try {
      for ( ConfigurablePropertyBuilder b : builders ) {
        try {
          ConfigurableProperty prop = null;
          try {
            prop = b.buildProperty( c, field );
          } catch ( ConfigurablePropertyException e ) {
            throw e;
          } catch ( Exception t ) {
            LOG.error( "Failed to prepare configurable field: " + c.getCanonicalName( ) + "." + field.getName( ), t );
          }
          if ( prop != null ) {
            if ( prop.isDeferred( ) ) {
              if ( !fqPendingMap.containsKey( prop.getQualifiedName( ) ) ) {
                fqPendingMap.put( prop.getQualifiedName( ), prop );
                fqPendingPrefixMap.put( prop.getEntrySetName( ), prop );
                return prop;
              }
            } else {
              if ( !fqMap.containsKey( prop.getQualifiedName( ) ) ) {
                fqMap.put( prop.getQualifiedName( ), prop );
                fqPrefixMap.put( prop.getEntrySetName( ), prop );
                return prop;
              } else {
                RuntimeException r = new RuntimeException( "Duplicate configurable field in same config file: \n" + "-> "
                    + fqMap.get( prop.getQualifiedName( ) ).getDefiningClass( ).getCanonicalName( ) + "."
                    + field.getName( )
                    + "\n" + "-> " + c.getCanonicalName( ) + "." + field.getName( ) + "\n" );
                LOG.fatal( r, r );
                throw r;
              }
            }
          }
        } catch ( ConfigurablePropertyException e ) {
          LOG.debug( e, e );
        }
      }
    } finally {
      fqLock.writeLock().unlock();
    }
    return null;
  }
  
  public static List<String> getPropertyEntrySetNames( ) {
    fqLock.readLock().lock();
    try {
      return Lists.newArrayList( fqPrefixMap.keySet( ) );
    } finally {
      fqLock.readLock().unlock();
    }
  }
  
  public static List<ConfigurableProperty> getPropertyEntrySet( ) {
    fqLock.readLock().lock();
    try {
      List<ConfigurableProperty> props = Lists.newArrayList( );
      for ( String fqPrefix : fqPrefixMap.keySet( ) ) {
        props.addAll( getPropertyEntrySet( fqPrefix ) );
      }
      return props;
    } finally {
      fqLock.readLock().unlock();
    }
  }
  
  public static List<ConfigurableProperty> getPropertyEntrySet( String prefix ) {
    fqLock.readLock().lock();
    try {
      List<ConfigurableProperty> props = Lists.newArrayList( );
      for ( ConfigurableProperty fq : fqPrefixMap.get( prefix ) ) {
        props.add( fq );
      }
      return props;
    } finally {
      fqLock.readLock().unlock();
    }
  }
  
  public static List<ConfigurableProperty> getPropertyEntrySet( String prefix, String alias ) {
    fqLock.readLock().lock();
    try {
      List<ConfigurableProperty> props = Lists.newArrayList( );
      for ( ConfigurableProperty fq : fqPrefixMap.get( prefix ) ) {
        if ( fq.getAlias( ).equals( alias ) )
          props.add( fq );
      }
      return props;
    } finally {
      fqLock.readLock().unlock();
    }
  }
    
  public static ConfigurableProperty getPropertyEntry( String fq ) throws IllegalAccessException {
    fqLock.readLock().lock();
    try {
      if ( !fqMap.containsKey( fq ) ) {
        throw new IllegalAccessException( "No such property: " + fq );
      } else {
        return fqMap.get( fq );
      }
    } finally {
      fqLock.readLock().unlock();
    }
  }
  
  public static Collection<Entry<String,ConfigurableProperty>> getPendingPropertyEntries( ) {
    fqLock.readLock().lock();
    try {
      return fqPendingPrefixMap.entries( );
    } finally {
      fqLock.readLock().unlock();
    }
  }

  public static List<ConfigurableProperty> getPendingPropertyEntrySet( String prefix ) {
    fqLock.readLock().lock();
    try {
      List<ConfigurableProperty> props = Lists.newArrayList( );
      for ( ConfigurableProperty fq : fqPendingPrefixMap.get( prefix ) ) {
        props.add( fq );
      }
      return props;
    } finally {
      fqLock.readLock().unlock();
    }
  }

  public static String getEntrySetDescription( String entrySetName ) {
    return "Temporary description";
  }
  
  public static List<ComponentProperty> getComponentPropertySet( String prefix ) {
    List<ComponentProperty> componentProps = Lists.newArrayList( );
    List<ConfigurableProperty> props = getPropertyEntrySet( prefix );
    for ( ConfigurableProperty prop : props ) {
      componentProps.add( new ComponentProperty( prop.getWidgetType( ).toString( ), prop.getDisplayName( ), prop.getValue( ), prop.getQualifiedName( ) ) );
    }
    return componentProps;
  }
  
  public static boolean addProperty( ConfigurableProperty prop ) {
    fqLock.writeLock().lock();
    try {
      if ( !fqMap.containsKey( prop.getQualifiedName( ) ) ) {
        fqMap.put(prop.getQualifiedName(), prop);
        fqPrefixMap.put( prop.getEntrySetName(), prop );
        return true;
      } else {
        return false;
      }
    } finally {
      fqLock.writeLock().unlock();
    }
  }
  
  public static void removeProperty( ConfigurableProperty prop ) {
    fqLock.writeLock().lock();
    try {
      if ( fqMap.containsKey( prop.getQualifiedName( ) ) ) {
        fqMap.remove( prop.getQualifiedName( ) );
        fqPrefixMap.remove( prop.getEntrySetName( ), prop );
      }
    } finally {
      fqLock.writeLock().unlock();
    }
  }
  
  public static List<ComponentProperty> getComponentPropertySet( String prefix, String alias ) {
    List<ComponentProperty> componentProps = Lists.newArrayList( );
    List<ConfigurableProperty> props = getPropertyEntrySet( prefix, alias );
    for ( ConfigurableProperty prop : props ) {
      componentProps.add( new ComponentProperty( prop.getWidgetType( ).toString( ), prop.getDisplayName( ), prop.getValue( ), prop.getQualifiedName( ) ) );
    }
    return componentProps;
  }
}
