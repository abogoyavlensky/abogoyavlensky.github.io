Coding agents are great, and they get even more useful when you can run them with broad permissions. But that also makes isolation the hard part, so I've been looking for a convenient and reliable way to sandbox them.

### First attempts

I've tried a few things so far. The most obvious starting point was [Docker containers](https://www.docker.com/). They are lightweight and still a reasonable option, but they become less comfortable when you need Docker-in-Docker.

For example, tests that use Testcontainers need access to a Docker daemon from inside the container. If that daemon is exposed through the host Docker socket, the agent effectively gets broad host-level privileges. The container boundary is also thinner than I want here: shared kernel, syscall exposure, and container escape CVEs.

So I kept searching and tried [Docker Sandboxes](https://docs.docker.com/ai/sandboxes/). It might not be the most obvious choice, since the project is new and still experimental, but it was on my radar, and it worked well. The idea is to spin up a new microVM per project.

It has a few default templates for coding agents, or you can configure a custom one. That gives you real VM isolation. Docker is available inside the VM out of the box, without restrictions or shortcuts. It is also relatively easy to spin up.

However, a few downsides annoyed me:
- VM creation is fairly heavy and requires at least 10-15 GB of disk space.
- Mounts are inflexible: they are frozen at creation, and you cannot change mounted directories after the VM exists. There is also no configurable mount point inside the VM for shared directories.
- Environment variables are limited: only a few approved variables are passed through for agent login, so everything else has to be copied manually into the terminal.
- HTTP proxying makes even dependency fetching painful.

Verdict: right isolation level, but too heavy and too inflexible for daily use.

### Searching for alternatives

Then I looked at the alternatives I could find and split them into two groups:

1. VMs and microVMs:
    - [Vagrant](https://developer.hashicorp.com/vagrant) - classic VM orchestrator, but heavy and less flexible these days.
    - [Lima](https://lima-vm.io/) - lightweight Linux VMs.
    - [Apple Container](https://github.com/apple/container) - Apple's native container runtime. It looks promising, but Docker-in-Docker is not straightforward, if it is possible at all. In day-to-day use, it also feels closer to Docker containers than to a VM sandbox.
    - [Lume](https://cua.ai/docs/lume/guide/getting-started/introduction)/[Tart](https://tart.run/)/[vfkit](https://github.com/crc-org/vfkit) - lower-level Apple Virtualization.framework wrappers.
    - [vibe](https://github.com/lynaghk/vibe)/[vibebox](https://vibebox.robcholz.com/)/[Shuru](https://shuru.run/)/... - modern attempts to solve agent sandboxing with microVMs or Docker containers. They are moving in the right direction, but may still be early.

2. Non-VM sandboxing:
    - [sandbox-exec](https://igorstechnoclub.com/sandbox-exec/) - macOS's built-in sandbox tool.
    - [bubblewrap](https://github.com/containers/bubblewrap) - lightweight Linux namespace sandbox.
    - [Agent Safehouse](https://agent-safehouse.dev/) - wrapper for macOS sandbox-exec with composable policy profiles.
    - [Zerobox](https://github.com/afshinm/zerobox)/... - other tools built around configurable namespace isolation.

### My go-to solution: Lima

I am most comfortable with VM or microVM isolation, so I tried a few more options from that category and ended up choosing Lima as my go-to solution for local agent sandboxing.

Lima runs lightweight Linux VMs. On macOS, it uses Apple's Virtualization.framework. It gives me the same isolation model as Docker Sandboxes, without the pain.

What I like about Lima's user experience:
- Fast VM creation and reasonable defaults.
- Mounts can be changed after VM creation.
- Custom mount points inside the VM are possible, which is useful for sharing skills.
- Disk grows on demand.
- Full control over environment variables.
- Scriptable setup.
- By default, it exposes servers running on localhost inside the VM to the host system, so manual testing of web apps is trivial.

Some minor cons:
- Mount points inside the VM are configured only through the YAML file; I didn't find a way to set them from the CLI alone.
- Egress control is still unsolved. You have to figure out how to restrict outgoing HTTP requests yourself: local proxies, `/etc/hosts`, or something else.

### My Lima setup

In my setup, I ended up creating one VM for all projects that live under a single directory on my host machine, such as `/path/to/Projects`.

I tried a VM per project, but it wasn't convenient because I sometimes want to add more directories to the agent's context. I also don't see much value in that level of granularity when git worktrees already cover parallel work on different features in the same project.

So I have one development VM where I mount all my projects and my skills directory. I intentionally don't share full agent config directories, such as `~/.claude` or `~/.codex`; I keep them inside the VM.

I created a custom startup script [agent.yaml](https://github.com/abogoyavlensky/agents/blob/master/sandbox/agent.yaml) with a few useful system dependencies. It installs [mise](https://mise.jdx.dev/), so I can add tools conveniently inside the VM, plus Homebrew on Linux and a few popular coding agents, and it sets up some shell aliases.

You can start from scratch with any official Lima template, customize your own, or try my template linked above.

I also have a few convenient [shell functions](https://github.com/abogoyavlensky/agents/blob/master/sandbox/README.md?plain=1#L59-L86) that simplify running agents through the sandboxed VM. The goal is to make the VM invisible: right now there is no UX difference between running `claude` directly and running `lmcc`. Same speed and convenience, but a completely different level of isolation.

### Workflow

Now let's look at my typical workflow for using an agent with Lima.

First, install Lima on your host machine:

```bash
brew install lima
```

For the simplest setup, use one of the built-in templates:

```bash
limactl create --name sandbox template:docker
```

`--name` can be anything you want.

Alternatively, for a more complete setup, use my startup script:

```bash
curl -fsSL -o agent.yaml https://raw.githubusercontent.com/abogoyavlensky/agents/master/sandbox/agent.yaml
limactl create --name sandbox ./agent.yaml
```

Once the VM is created, mount your projects directory. `cd` into it, then add it as a mount and start the VM:

```bash
cd ~/Projects   # or any directory you want to expose
limactl edit sandbox --mount-only .:w --start
```

You can add more directories by stopping the VM and extending the `mounts` section in the YAML file that `limactl edit` opens:

```bash
limactl stop sandbox
limactl edit sandbox
```

If your skills directory is outside the `Projects` directory you mounted above, add this to the `mounts` section:

```yaml
mounts:
  # ...
  - location: "/path/to/agents/skills"
    mountPoint: "/home/agent.guest/.claude/skills"
    writable: true
  - location: "/path/to/agents/skills"
    mountPoint: "/home/agent.guest/.agents/skills"
    writable: true
```

Or, if your skills directory is inside `Projects`, link it to the right place inside the VM:

```bash
ln -s /path/to/Projects/agents/skills ~/.claude/skills
ln -s /path/to/Projects/agents/skills ~/.agents/skills
```

Now you can open a shell inside the VM from your current host directory, as long as that directory is inside one of the mounted directories:

```bash
limactl shell sandbox
```

Set your git identity inside the VM:

```bash
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"
```

You can also run a command inside the VM from your host system:

```bash
limactl shell sandbox -- "claude"
```

Claude Code will start inside the VM, and you can use it as usual.

If you want to propagate only some environment variables to the VM, do it like this:

```bash
LIMA_SHELLENV_BLOCK=* LIMA_SHELLENV_ALLOW=GITHUB_TOKEN limactl shell --preserve-env sandbox
```

The first variable, `LIMA_SHELLENV_BLOCK`, blocks all preserved environment variables from the host system. The second, `LIMA_SHELLENV_ALLOW`, allows only the specific variables you want to propagate.

With that in mind, we can set up a few small, useful aliases:

```bash
LIMA_DEFAULT_VM="sandbox"

# Run any command inside the VM within the current host directory.
lm() {
  limactl shell "$LIMA_DEFAULT_VM" -- "$@"
}

# Open a shell in the VM within the current host directory.
lmsh() {
  limactl shell "$LIMA_DEFAULT_VM"
}

# Run Claude Code with all permissions skipped.
lmcc() {
  lm claude --dangerously-skip-permissions "$@"
}

# Run Codex CLI with all permissions skipped.
lmcx() {
  lm codex --yolo "$@"
}

# Run OpenCode.
lmoc() {
  lm opencode "$@"
}

# Run pi.
lmpi() {
  lm pi "$@"
}
```

The next time you want to run Claude Code, run:

```bash
lmcc
```

You will get the same experience as running `claude` directly, but inside an isolated VM sandbox.

### Takeaways

- It would be useful to allowlist outgoing HTTP requests, or at least have a clear way to control them. For now, I use `/etc/hosts` to restrict some domains: `127.0.0.1 some.domain.com`.
- Some agent orchestration tools do not work with VMs. If you share agent config directories from the host to the VM, more of those tools may work.
- I find myself doing more and more inside the VM, not just agent-related tasks. I run and install many other tools there too, which keeps my host machine clean.
