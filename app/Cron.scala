package lila.http

import play.api._
import play.api.libs.concurrent.Akka
import akka.actor._
import akka.util.duration._
import akka.util.Duration
import scalaz.effects._

import lila.system.SystemEnv

final class Cron(env: SystemEnv)(implicit app: Application) {

  spawn("online_username") { env ⇒
    env.userRepo updateOnlineUsernames env.usernameMemo.keys
  }

  spawn("hook_cleanup_dead") { env ⇒
    env.hookRepo keepOnlyOwnerIds env.hookMemo.keys flatMap { hasRemoved ⇒
      if (hasRemoved) (env.lobbyMemo ++) map (_ ⇒ Unit) else io()
    }
  }

  spawn("hook_cleanup_old") { env ⇒
    env.hookRepo.cleanupOld
  }

  spawn("game_cleanup_unplayed") { env ⇒
    putStrLn("[cron] remove old unplayed games") flatMap { _ ⇒
      env.gameRepo.cleanupUnplayed
    }
  }

  spawn("game_auto_finish") { env ⇒
    for {
      games ← env.gameRepo.candidatesToAutofinish
      _ ← putStrLn("[cron] finish %d games (%s)".format(
        games.size, games take 3 mkString ", "))
      _ ← env.finisher outoftimes games
    } yield ()
  }

  def spawn(name: String)(f: SystemEnv ⇒ IO[Unit]) = {
    val freq = env.getMilliseconds("cron.%s.frequency" format name) millis
    val actor = Akka.system.actorOf(Props(new Actor {
      def receive = {
        case "tick" ⇒ f(env).unsafePerformIO
      }
    }), name = name)
    Akka.system.scheduler.schedule(freq, freq, actor, "tick")
  }
}
