Coding agents are great, and they get even more useful when you let them run with broad permissions. In practice that means full-auto mode, like `claude --dangerously-skip-permissions` or `codex --yolo`, where the agent runs any command without stopping to ask.

That is also what makes it risky: an agent in that mode can delete files, read secrets, or make network calls on its own. So isolation becomes the hard part, and I've been looking for a convenient and reliable way to sandbox them.

### TL;DR

Run coding agents in full-auto mode inside a disposable [Lima](https://lima-vm.io/) VM: VM-level isolation, with the day-to-day convenience of running them directly on your machine.

### First attempts

I've tried a few things so far. The most obvious starting point was [Docker containers](https://www.docker.com/). They are lightweight and still a reasonable option, but they become less comfortable when you need Docker-in-Docker.

For example, tests that use Testcontainers need access to a Docker daemon from inside the container. If that daemon is exposed through the host Docker socket, the agent effectively gets broad host-level privileges. The container boundary is also thinner than I want here: shared kernel, syscall exposure, and container escape CVEs.

So I kept searching and tried [Docker Sandboxes](https://docs.docker.com/ai/sandboxes/). It might not be the most obvious choice, since the project is new and still experimental, but it was on my radar, and it worked well. The idea is to spin up a new microVM per project.

It has a few default templates for coding agents, or you can configure a custom one. That gives you real VM isolation. Docker is available inside the VM out of the box, without restrictions or shortcuts. It is also relatively easy to spin up.

However, a few downsides annoyed me:
- VM creation is fairly heavy and requires at least 10-15 GB of disk space.
- Mounts are inflexible: they are frozen at creation, and you cannot change mounted directories after the VM exists. There is also no configurable mount point inside the VM for shared directories.
- Environment variables are limited: only a few approved variables are passed through for agent login, so everything else has to be copied manually into the terminal.
- HTTP proxying makes dependency fetching painful.

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
- Fast VM creation with reasonable defaults (4 CPUs and 4 GB RAM in my template).
- Mounts can be changed after VM creation.
- Custom mount points inside the VM are possible, which is useful for sharing skills.
- Disk grows on demand, so the VM starts small instead of reserving 10-15 GB up front like a Docker Sandbox.
- Full control over environment variables.
- Scriptable setup.
- By default, it exposes servers running on localhost inside the VM to the host system, so manual testing of web apps is trivial.

Some minor cons:
- Mount points inside the VM are configured only through the YAML file; I didn't find a way to set them from the CLI alone.
- Egress control is still unsolved, so restricting outgoing requests is left to you.

### My Lima setup

In my setup, I ended up creating one VM for all projects that live under a single directory on my host machine, such as `/path/to/Projects`.

I tried a VM per project, but it wasn't convenient because I sometimes want to add more directories to the agent's context and it breaks flow a bit. I also don't see much value in that level of granularity when git worktrees already cover parallel work on different features in the same project.

If you really want to have a VM per project it's possible with Lima as well and can be achived with the same speed and conveninence using [clone](https://lima-vm.io/docs/reference/limactl_clone/) VM command.

So I have one development VM where I mount all my projects and my skills directory. I intentionally don't share full agent config directories, such as `~/.claude` or `~/.codex`; I keep them inside the VM.

I created a custom startup script with a few useful system dependencies. It installs [mise](https://mise.jdx.dev/), so I can add tools conveniently inside the VM, plus Homebrew on Linux and a few popular coding agents, and it sets up some shell aliases.

You can start from scratch with any official Lima template, customize your own, or try my template linked above.

I also have a few convenient shell functions that simplify running agents through the sandboxed VM. The goal is to make the VM invisible: right now there is no UX difference between running `claude` directly and running an alias, say: `lmcc`. Same speed and convenience, but a completely different level of isolation.

### Workflow

Now let's look at my typical workflow for using an agent with Lima.

First, install Lima on your host machine:

```bash
brew install lima
```

For the simplest setup, use one of the [built-in templates](https://lima-vm.io/docs/templates/):

```bash
limactl create --name sandbox template:docker
```

`--name` can be anything you want. The built-in Docker template is enough to follow the rest of this post.

For a more complete setup, you can use my custom template instead. It bundles mise, Homebrew on Linux, a few coding agents, and some shell aliases. Copy the snapshot below into a local `agent.yaml`:

<details>
<summary>agent.yaml</summary>

```yaml
minimumLimaVersion: "2.0.0"

base:
  - template:docker

vmType: "vz"
mountType: "virtiofs"

cpus: 4
memory: "4GiB"
disk: "60GiB"

user:
  name: agent


provision:
  - mode: system
    script: |
      set -eux
      apt-get update
      apt-get install -y \
        software-properties-common curl git unzip zip gnupg gnupg2 rlwrap \
        apt-transport-https ca-certificates wget bzip2 nano libssl-dev \
        build-essential pkg-config ripgrep zlib1g-dev procps file fzf \
        openssh-client jq gh gpg-agent iproute2

  - mode: user
    script: |
      set -eux

      # Install mise (user-level, idempotent)
      if [ ! -x "$HOME/.local/bin/mise" ]; then
        curl https://mise.run | sh
      fi

      # Login shell profile: PATH for mise shims, ~/.local/bin, linuxbrew;
      # source .bashrc so interactive features (mise activate) also load.
      cat > "$HOME/.bash_profile" <<'EOF'
      # Homebrew (linuxbrew)
      if [ -x /home/linuxbrew/.linuxbrew/bin/brew ]; then
        export PATH="/home/linuxbrew/.linuxbrew/bin:/home/linuxbrew/.linuxbrew/sbin:$PATH"
      fi

      # User-local binaries (mise itself, etc.)
      export PATH="$HOME/.local/bin:$PATH"

      # mise-managed tools (shims must be on PATH for non-interactive shells)
      export PATH="$HOME/.local/share/mise/shims:$PATH"

      # aliases
      alias gs="git status"
      alias gcam='git commit -v -am'
      alias gcb='git checkout -b $1'

      # Source .bashrc for interactive setup (mise activate, aliases, etc.)
      [ -f ~/.bashrc ] && . ~/.bashrc
      EOF

      # Interactive bash: mise activate + same PATH guards (idempotent appends)
      grep -q 'mise activate bash' "$HOME/.bashrc" || \
        echo 'eval "$($HOME/.local/bin/mise activate bash)"' >> "$HOME/.bashrc"
      grep -q 'linuxbrew/.linuxbrew/bin' "$HOME/.bashrc" || \
        echo 'export PATH="/home/linuxbrew/.linuxbrew/bin:/home/linuxbrew/.linuxbrew/sbin:$PATH"' >> "$HOME/.bashrc"
      grep -q 'mise/shims' "$HOME/.bashrc" || \
        echo 'export PATH="$HOME/.local/bin:$PATH"' >> "$HOME/.bashrc"
      grep -q 'mise/shims' "$HOME/.bashrc" || \
        echo 'export PATH="$HOME/.local/share/mise/shims:$PATH"' >> "$HOME/.bashrc"

      # Make mise + shims usable in this provisioning shell
      export PATH="$HOME/.local/bin:$HOME/.local/share/mise/shims:$PATH"

      # Skills directories for agents
      mkdir -p "$HOME/.claude/skills" "$HOME/.agents/skills"

      # Homebrew (linuxbrew), non-interactive.
      BREW_PREFIX="/home/linuxbrew/.linuxbrew"
      if [ ! -x "$BREW_PREFIX/bin/brew" ]; then
        NONINTERACTIVE=1 /bin/bash -c \
          "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
      fi
      export PATH="$BREW_PREFIX/bin:$BREW_PREFIX/sbin:$PATH"

      # AI agent CLIs and other tools
      brew install codex
      brew install anomalyco/tap/opencode

      # Claude Code via the official installer
      if ! command -v claude >/dev/null 2>&1; then
        curl -fsSL https://claude.ai/install.sh | bash
      fi
```

</details>

Then create the VM from your local copy:

```bash
limactl create --name sandbox ./agent.yaml
```

Once the VM is created, mount your projects directory. `cd` into it, then add it as a mount and start the VM:

```bash
cd ~/Projects   # or any directory you want to expose
limactl edit sandbox --mount-only .:w --start
```

By default, inside the VM the directory is mounted at exactly the same path as on your host.

You can add more directories by stopping the VM and extending the `mounts` section in the YAML file that `limactl edit` opens:

```bash
limactl stop sandbox
limactl edit sandbox
```

Optionally, share a skills directory with the VM. If it lives outside the `Projects` directory you mounted above, add it to the `mounts` section:

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

Or, if it already lives inside `Projects`, symlink it into place once you have a shell in the VM (see below):

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

Now being at the project you are working on, you can also run a command inside the VM from your host system:

```bash
limactl shell sandbox -- "claude"
```

Claude Code will start inside the VM, and you can login once and use it as usual.

If you want to propagate only some environment variables to the VM, do it like this:

```bash
LIMA_SHELLENV_BLOCK=* LIMA_SHELLENV_ALLOW=GITHUB_TOKEN limactl shell --preserve-env sandbox
```

The first variable, `LIMA_SHELLENV_BLOCK`, blocks all preserved environment variables from the host system. The second, `LIMA_SHELLENV_ALLOW`, allows only the specific variables you want to propagate.

Forwarding `GITHUB_TOKEN` this way is also how I authenticate inside the VM. With it set, `gh` and `git push` just work, so I don't have to copy SSH keys or log in again.

With that in mind, we can set up a few small, useful aliases:

```bash
LIMA_DEFAULT_VM="sandbox"

# Open a shell in the VM within the current host directory.
lmsh() {
  limactl shell "$LIMA_DEFAULT_VM"
}

# Run any command inside the VM within the current host directory.
lm() {
  limactl shell "$LIMA_DEFAULT_VM" -- "$@"
}

# Run Claude Code with all permissions skipped.
lmcc() {
  lm claude --dangerously-skip-permissions "$@"
}

# Run Codex CLI with all permissions skipped.
lmcx() {
  lm codex --yolo "$@"
}

# Run OpenCode
lmoc() {
  lm opencode "$@"
}
```

The next time you want to run Claude Code, run:

```bash
lmcc
```

You will get the same experience as running `claude` directly, but inside an isolated VM sandbox.

If a VM ever ends up in a bad state, it is disposable. Delete it and recreate it from the template:

```bash
limactl stop sandbox
limactl delete sandbox
```

### What it protects, and what it doesn't

The VM is not a perfect boundary, so it helps to be clear about what it actually gives you.

It protects your host OS and everything outside the mounts. In the worst case, a misbehaving agent trashes the VM, and you throw it away and start over.

It does not protect what you hand to the agent. Your projects are mounted writable, so the agent can still modify or delete those files on the host. Any secret you forward, such as `GITHUB_TOKEN`, is usable inside the VM too. So mount only what the agent needs, and forward only the secrets it needs.

Egress is the other gap. Nothing stops the agent from reaching the network, so for now I use `/etc/hosts` to block some domains: `127.0.0.1 some.domain.com`. A proper allowlist for outgoing requests would be better, but I haven't found a setup I'm happy with yet.

### Takeaways

- Some agent orchestration tools do not work with VMs. If you share agent config directories from the host to the VM, more of those tools may work. Also it's possible to connect to the VM via SSH, so it also might be helpful.
- I find myself doing more and more inside the VM, not just agent-related tasks. I run and install many other tools there too, which keeps my host machine clean.

For me, Lima hits the sweet spot: VM-grade isolation that still feels like running agents directly. It is my default now, and I don't see myself going back to running agents straight on the host.
