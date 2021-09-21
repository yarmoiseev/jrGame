package com.game.service;

import com.game.entity.Player;
import com.game.entity.Profession;
import com.game.entity.Race;
import com.game.exception.BadRequestException;
import com.game.exception.NotFoundException;
import com.game.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.xml.bind.ValidationException;
import java.util.Date;
import java.util.List;

@Service
public class PlayerServiceImpl implements PlayerService {
    private PlayerRepository playerRepository;

    @Autowired
    public void setPlayerRepository(PlayerRepository playerRepository) {this.playerRepository = playerRepository;}

    @Override
    public Page<Player> findAllPlayers(Specification<Player> specification, Pageable pageable) {
        return playerRepository.findAll(specification, pageable);
    }

    @Override
    public List<Player> findAllPlayers(Specification<Player> specification) {
        return playerRepository.findAll(specification);
    }

    @Override
    public Player createPlayer(Player player) {
        if (!isValidPlayer(player))
            throw new BadRequestException("Some fields are filled in incorrectly or empty");

        if (player.getBanned() == null)
            player.setBanned(false);

        updateLvlAndUntilNextLvl(player);

        return playerRepository.save(player);
    }

    @Override
    public Player updatePlayer(Long id, Player player) {
        Player updatedPlayer = findPlayerByID(id);

        if (player.getName() != null) updatedPlayer.setName(player.getName());
        if (player.getTitle() != null) updatedPlayer.setTitle(player.getTitle());
        if (player.getRace() != null) updatedPlayer.setRace(player.getRace());
        if (player.getProfession() != null) updatedPlayer.setProfession(player.getProfession());
        if (player.getBanned() != null) updatedPlayer.setBanned(player.getBanned());
        if (player.getBirthday() != null) {
            if (!isValidDate(player.getBirthday()))
                throw new BadRequestException("Wrong date of birth");
            updatedPlayer.setBirthday(player.getBirthday());
        }
        if (player.getExperience() != null) {
            if (!isValidExperience(player.getExperience()))
                throw new BadRequestException("Unacceptable experience value");
            updatedPlayer.setExperience(player.getExperience());
            updateLvlAndUntilNextLvl(updatedPlayer);
        }
        return playerRepository.save(updatedPlayer);
    }

    @Override
    public void deletePlayer(Long id) {
        findPlayerByID(id);
        playerRepository.deleteById(id);

    }

    @Override
    public Player findPlayerByID(Long id) {
        if (id > Long.MAX_VALUE || id <= 0) throw new BadRequestException("Wrong id");
        return playerRepository.findById(id).orElseThrow(() -> new NotFoundException("Player not found"));
    }



    /*--------------------Validation block--------------------*/

    private boolean isValidPlayer(Player player) {
        return player != null && player.getRace() != null && player.getProfession() != null
                && isValidName(player.getName()) && isValidTitle(player.getTitle()) &&
                isValidExperience(player.getExperience()) && isValidDate(player.getBirthday());
    }

    private boolean isValidName(String name) { return name != null && name.length() <= 12 && !name.isEmpty(); }

    private boolean isValidTitle(String title) { return title != null && title.length() <= 30; }

    private boolean isValidExperience(Integer experience) {
        return experience != null && experience > 0 && experience <= 10_000_000;
    }

    private boolean isValidDate(Date date) {
        return date != null && date.getTime() >= 0 &&
                date.getTime() >= 946674000482L && date.getTime() <= 32535205199494L;
    }

    /*--------------------End of validation block-------------*/

    private void updateLvlAndUntilNextLvl(Player player) {
        int level = (int) (Math.sqrt(2500 + 200 * player.getExperience()) - 50) / 100;
        int untilNextLevel = 50 * (level + 1) * (level + 2) - player.getExperience();
        player.setLevel(level);
        player.setUntilNextLevel(untilNextLevel);
    }

    /*--------------------Filters block-------------*/
    @Override
    public Specification<Player> filterByName(String name) {
        return (root, criteriaQuery, criteriaBuilder) ->
                name == null ? null : criteriaBuilder.like(root.get("name"), "%" + name + "%");

    }

    @Override
    public Specification<Player> filterByTitle(String title) {
        return (root, criteriaQuery, criteriaBuilder) ->
                title == null ? null : criteriaBuilder.like(root.get("title"), "%" + title + "%");
    }

    @Override
    public Specification<Player> filterByRace(Race race) {
        return (root, criteriaQuery, criteriaBuilder) ->
                race == null ? null : criteriaBuilder.equal(root.get("race"), race);
    }

    @Override
    public Specification<Player> filterByProfession(Profession profession) {
        return (root, criteriaQuery, criteriaBuilder) ->
                profession == null ? null : criteriaBuilder.equal(root.get("profession"), profession);
    }

    @Override
    public Specification<Player> filterByDate(Long after, Long before) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (after == null && before == null)
                return null;
            else if (after == null)
                return criteriaBuilder.lessThanOrEqualTo(root.get("birthday"), new Date(before));
            else if (before == null)
                return criteriaBuilder.greaterThanOrEqualTo(root.get("birthday"), new Date(after));
            else
                return criteriaBuilder.between(root.get("birthday"), new Date(after), new Date(before));
        };
    }

    @Override
    public Specification<Player> filterByBanned(Boolean banned) {
        return (root, criteriaQuery, criteriaBuilder) ->
                banned == null ? null :
                        banned ? criteriaBuilder.isTrue(root.get("banned")) : criteriaBuilder.isFalse(root.get("banned"));
    }

    @Override
    public Specification<Player> filterByExperience(Integer minExperience, Integer maxExperience) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (minExperience == null && maxExperience == null)
                return null;
            else if (minExperience == null)
                return criteriaBuilder.lessThanOrEqualTo(root.get("experience"), maxExperience);
            else if (maxExperience == null)
                return criteriaBuilder.greaterThanOrEqualTo(root.get("experience"), minExperience);
            else
                return criteriaBuilder.between(root.get("experience"), minExperience, maxExperience);
        };
    }

    @Override
    public Specification<Player> filterByLevel(Integer minLevel, Integer maxLevel) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (minLevel == null && maxLevel == null)
                return null;
            else if (minLevel == null)
                return criteriaBuilder.lessThanOrEqualTo(root.get("level"), maxLevel);
            else if (maxLevel == null)
                return criteriaBuilder.greaterThanOrEqualTo(root.get("level"), minLevel);
            else
                return criteriaBuilder.between(root.get("level"), minLevel, maxLevel);
        };
    }
    /*--------------------And of filters block-------------*/
}
