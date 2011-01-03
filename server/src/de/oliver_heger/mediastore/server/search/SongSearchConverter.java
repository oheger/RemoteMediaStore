package de.oliver_heger.mediastore.server.search;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;

import de.oliver_heger.mediastore.server.db.EntityManagerSupport;
import de.oliver_heger.mediastore.server.model.ArtistEntity;
import de.oliver_heger.mediastore.server.model.SongEntity;
import de.oliver_heger.mediastore.service.utils.DTOTransformer;
import de.oliver_heger.mediastore.shared.model.SongInfo;

/**
 * <p>
 * A specialized {@link SearchConverter} implementation that deals with song
 * objects.
 * </p>
 * <p>
 * In addition to the converter functionality, this class also has to resolve
 * some IDs to populate all fields of the {@link SongInfo} object.
 * </p>
 *
 * @author Oliver Heger
 * @version $Id: $
 */
public class SongSearchConverter implements
        SearchConverter<SongEntity, SongInfo>, EntityManagerSupport
{
    /** A map with the already resolved artist entities. */
    private Map<Long, ArtistEntity> artistEntities;

    /** The entity manager for resolving references. */
    private EntityManager em;

    /**
     * Initializes this object with a list of artist entities. These artists are
     * used to resolve the IDs stored in the song entity objects.
     *
     * @param artists a list of artist objects
     */
    public void initResolvedArtists(Collection<? extends ArtistEntity> artists)
    {
        artistEntities = new HashMap<Long, ArtistEntity>();
        for (ArtistEntity ae : artists)
        {
            artistEntities.put(ae.getId(), ae);
        }
    }

    /**
     * Returns the entity manager used by this converter.
     *
     * @return the entity manager
     */
    public EntityManager getEntityManager()
    {
        return em;
    }

    /**
     * Initializes the entity manager. This entity manager is needed for
     * resolving references to other objects.
     *
     * @param em the entity manager
     */
    @Override
    public void setEntityManager(EntityManager em)
    {
        this.em = em;
    }

    /**
     * Converts the given song entity object to a song info object.
     *
     * @param e the entity to be converted
     * @return the resulting info object
     */
    @Override
    public SongInfo convert(SongEntity e)
    {
        SongInfo info = new SongInfo();
        DTOTransformer.transform(e, info);

        ArtistEntity ae = resolveArtist(e);
        if (ae == null)
        {
            info.setArtistID(null);
        }
        else
        {
            info.setArtistName(ae.getName());
        }

        return info;
    }

    /**
     * Resolves the artist of the specified song. If a list of artist entities
     * has been set, this information is used to resolve the entity. Otherwise,
     * the ID is looked up using the entity manager.
     *
     * @param song the song entity
     * @return the resolved artist entity or <b>null</b> if the artist cannot be
     *         resolved
     */
    ArtistEntity resolveArtist(SongEntity song)
    {
        if (song.getArtistID() == null)
        {
            return null;
        }

        if (artistEntities != null)
        {
            return artistEntities.get(song.getArtistID());
        }
        else
        {
            return getEntityManager().find(ArtistEntity.class,
                    song.getArtistID());
        }
    }
}
